import io

CRLF = "\r\n"
path = "core/network/src/commonMain/kotlin/org/meshtastic/core/network/repository/MQTTRepositoryImpl.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0
lines = content.split(CRLF)

start_idx = [i for i, l in enumerate(lines) if l.strip() == "<<<<<<< HEAD"]
end_idx = [i for i, l in enumerate(lines) if l.strip() == ">>>>>>> upstream/main"]
assert len(start_idx) == 1, f"expected 1 remaining <<<<<<< marker, found {len(start_idx)}"
assert len(end_idx) == 1, f"expected 1 remaining >>>>>>> marker, found {len(end_idx)}"
start, end = start_idx[0], end_idx[0]
print("replacing lines", start, "to", end, "(0-indexed)")

new_block = """\
    // Extracted from proxyMessageFlow to keep it under detekt's LongMethod limit. Retries the initial connect
    // with exponential backoff, building the SUBSCRIBE list only after a successful CONNECT -- see
    // [buildSubscriptions] for why the option set depends on the protocol version the broker actually accepted.
    // Once established, autoReconnect handles subsequent drops and re-subscribes internally.
    @Suppress("DEPRECATION") // json_enabled: only way to toggle MQTT JSON until the firmware/proto provides a replacement.
    private fun ProducerScope<MqttClientProxyMessage>.launchConnectRetryLoop(
        session: ActiveMqttSession,
        newClient: MqttClientSession,
        endpoint: MqttEndpoint,
        channelIds: List<String>,
        rootTopic: String,
        mqttConfig: ModuleConfig.MQTTConfig?,
    ): Job = launch(start = CoroutineStart.LAZY) {
        var reconnectDelay = INITIAL_RECONNECT_DELAY_MS
        while (isActiveSession(session)) {
            val result = safeCatching {
                if (!isActiveSession(session)) return@launch
                Logger.i {
                    if (buildConfigProvider.isDebug) "MQTT Connecting to $endpoint" else "MQTT Connecting..."
                }
                newClient.connect(endpoint)
                if (!isActiveSession(session)) return@launch
                // Built here, not before connect: the option set depends on the version the broker accepted.
                val subscriptions =
                    buildSubscriptions(
                        globalIds = channelIds,
                        rootTopic = rootTopic,
                        jsonEnabled = mqttConfig?.json_enabled == true,
                        version = newClient.negotiatedProtocolVersion,
                    )
                if (subscriptions.isNotEmpty()) {
                    Logger.d { "MQTT subscribing to ${subscriptions.size} topics" }
                    subscribe(session, subscriptions)
                }
                Logger.i { "MQTT connected and subscribed" }
            }
            val failure = result.exceptionOrNull()
            when {
                result.isSuccess -> return@launch

                failure is MqttException.ConnectionRejected && failure.isCredentialRejection() -> {
                    Logger.e(failure) { "MQTT connection rejected (unrecoverable), stopping" }
                    close(failure)
                    return@launch
                }

                else -> {
                    if (!isActiveSession(session)) return@launch
                    // Broker- and network-side failures are what this retry loop exists to absorb — an
                    // unreachable host, a TLS problem, a dropped connection, or a broker that violates the
                    // MQTT 5 spec (e.g. the topic-alias limit). None are defects in this app, and reporting
                    // every retry as a non-fatal drowned real regressions.
                    //
                    // Anything else landing here is unexpected — a fault in our own connect/subscribe setup
                    // rather than the peer's — so it keeps reporting.
                    if (failure.isExpectedMqttRetryFailure()) {
                        Logger.w(failure) { "MQTT connect failed, retrying in ${reconnectDelay}ms" }
                    } else {
                        Logger.e(failure) { "MQTT connect failed unexpectedly, retrying in ${reconnectDelay}ms" }
                    }
                    delay(reconnectDelay)
                    reconnectDelay =
                        (reconnectDelay * RECONNECT_BACKOFF_MULTIPLIER).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                }
            }
        }
    }

    // A refusal is the broker's verdict on a filter, not a failed attempt: the client keeps the filters it granted,
    // and retrying the connect would only draw the same SUBACK on a backoff for ever. Record it for the UI instead.
    private suspend fun subscribe(session: ActiveMqttSession, subscriptions: List<Subscription>) {
        try {
            session.client.subscribe(subscriptions)
        } catch (e: MqttException.SubscriptionRefused) {
            Logger.w {
                if (buildConfigProvider.isDebug) {
                    "MQTT broker refused ${e.refused.size} of ${subscriptions.size} topics: ${e.message}"
                } else {
                    "MQTT broker refused ${e.refused.size} of ${subscriptions.size} topics"
                }
            }
            session.subscriptionRefusal.value = e
        }
    }\
""".split("\n")

lines[start : end + 1] = new_block

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(CRLF.join(lines))
print("step3 ok, new line count:", len(lines))
