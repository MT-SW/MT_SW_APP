import io

CRLF = "\r\n"
path = "core/network/src/commonMain/kotlin/org/meshtastic/core/network/repository/MQTTRepositoryImpl.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old_lines, new_lines, label):
    old = CRLF.join(old_lines) + CRLF
    new = CRLF.join(new_lines) + CRLF
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# --- 1. imports ---
old1 = [
    "<<<<<<< HEAD",
    "import org.meshtastic.core.model.util.allChannelIds",
    "=======",
    "import org.meshtastic.core.model.NodeAddress",
    ">>>>>>> upstream/main",
]
new1 = [
    "import org.meshtastic.core.model.NodeAddress",
    "import org.meshtastic.core.model.util.allChannelIds",
]
content = replace_once(content, old1, new1, "imports")

# --- 2. channelIds + connectJob (one contiguous conflicted region spanning two markers pairs,
#     with an unconflicted comment+launch line in between) ---
old2 = [
    "<<<<<<< HEAD",
    "        // The Client Proxy only wants channels the device asked for downlink on (subscribeList); the Sniffer",
    "        // wants to observe everything published for this device's channels, so it ignores that flag entirely.",
    "        val channelIds = if (subscribeAllChannels) channelSet.allChannelIds else channelSet.subscribeList",
    "        val subscriptions: List<Subscription> = buildSniffSubscriptions(channelIds, rootTopic, mqttConfig)",
    "",
    "=======",
    ">>>>>>> upstream/main",
    "        // Collect from the SharedFlow before connecting to avoid missing retained messages",
    "        // that arrive immediately after SUBSCRIBE.",
    "        launch { newClient.messages.collect { msg -> processMessage(msg) } }",
    "",
    "        // Retry the initial connect with exponential backoff. Once established,",
    "        // autoReconnect handles subsequent drops and re-subscribes internally.",
    "<<<<<<< HEAD",
    "        val connectJob = launchConnectRetryLoop(session, newClient, endpoint, subscriptions)",
    "=======",
    "        val connectJob =",
    "            launch(start = CoroutineStart.LAZY) {",
    "                var reconnectDelay = INITIAL_RECONNECT_DELAY_MS",
    "                while (isActiveSession(session)) {",
    "                    val result = safeCatching {",
    "                        if (!isActiveSession(session)) return@launch",
    "                        Logger.i {",
    "                            if (buildConfigProvider.isDebug) \"MQTT Connecting to $endpoint\" else \"MQTT Connecting...\"",
    "                        }",
    "                        newClient.connect(endpoint)",
    "                        if (!isActiveSession(session)) return@launch",
    "                        // Built here, not before connect: the option set depends on the version the broker accepted.",
    "                        val subscriptions =",
    "                            buildSubscriptions(",
    "                                globalIds = channelSet.subscribeList,",
    "                                rootTopic = rootTopic,",
    "                                jsonEnabled = mqttConfig?.json_enabled == true,",
    "                                version = newClient.negotiatedProtocolVersion,",
    "                            )",
    "                        if (subscriptions.isNotEmpty()) {",
    "                            Logger.d { \"MQTT subscribing to ${subscriptions.size} topics\" }",
    "                            subscribe(session, subscriptions)",
    "                        }",
    "                        Logger.i { \"MQTT connected and subscribed\" }",
    "                    }",
    "                    val failure = result.exceptionOrNull()",
    "                    when {",
    "                        result.isSuccess -> return@launch",
    "",
    "                        failure is MqttException.ConnectionRejected && failure.isCredentialRejection() -> {",
    "                            Logger.e(failure) { \"MQTT connection rejected (unrecoverable), stopping\" }",
    "                            close(failure)",
    "                            return@launch",
    "                        }",
    "",
    "                        else -> {",
    "                            if (!isActiveSession(session)) return@launch",
    "                            // Broker- and network-side failures are what this retry loop exists to absorb — an",
    "                            // unreachable host, a TLS problem, a dropped connection, or a broker that violates the",
    "                            // MQTT 5 spec (e.g. the topic-alias limit). None are defects in this app, and reporting",
    "                            // every retry as a non-fatal drowned real regressions.",
    "                            //",
    "                            // Anything else landing here is unexpected — a fault in our own connect/subscribe setup",
    "                            // rather than the peer's — so it keeps reporting.",
    "                            if (failure.isExpectedMqttRetryFailure()) {",
    "                                Logger.w(failure) { \"MQTT connect failed, retrying in ${reconnectDelay}ms\" }",
    "                            } else {",
    "                                Logger.e(failure) {",
    "                                    \"MQTT connect failed unexpectedly, retrying in ${reconnectDelay}ms\"",
    "                                }",
    "                            }",
    "                            delay(reconnectDelay)",
    "                            reconnectDelay =",
    "                                (reconnectDelay * RECONNECT_BACKOFF_MULTIPLIER).coerceAtMost(MAX_RECONNECT_DELAY_MS)",
    "                        }",
    "                    }",
    "                }",
    "            }",
    ">>>>>>> upstream/main",
]
new2 = [
    "        // The Client Proxy only wants channels the device asked for downlink on (subscribeList); the Sniffer",
    "        // wants to observe everything published for this device's channels, so it ignores that flag entirely.",
    "        // Not resolved into a full subscription list here -- see [buildSubscriptions] for why that has to wait",
    "        // until after a successful CONNECT.",
    "        val channelIds = if (subscribeAllChannels) channelSet.allChannelIds else channelSet.subscribeList",
    "",
    "        // Collect from the SharedFlow before connecting to avoid missing retained messages",
    "        // that arrive immediately after SUBSCRIBE.",
    "        launch { newClient.messages.collect { msg -> processMessage(msg) } }",
    "",
    "        // Retry the initial connect with exponential backoff. Once established,",
    "        // autoReconnect handles subsequent drops and re-subscribes internally.",
    "        val connectJob = launchConnectRetryLoop(session, newClient, endpoint, channelIds, rootTopic, mqttConfig)",
]
content = replace_once(content, old2, new2, "channelIds+connectJob")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("step1+2 ok, new length:", len(content))
