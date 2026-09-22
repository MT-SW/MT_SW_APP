import io

CRLF = "\r\n"
path = "core/resources/src/commonMain/composeResources/values/strings.xml"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0

# Each resolution: exact list of lines to end up with for that conflict block (no markers).
resolutions = [
    # 1. co2 block: take upstream's correctly-encoded co2/co2_humidity/co2_temperature, keep our extra keys.
    [
        '    <string name="co2">CO₂</string>',
        '    <string name="co2_humidity">CO₂ Hum</string>',
        '    <string name="co2_temperature">CO₂ Temp</string>',
        '    <string name="codec_2_enabled">CODEC 2 enabled</string>',
        '    <string name="codec2_sample_rate">CODEC2 sample rate</string>',
        '    <string name="coding_rate">Coding Rate</string>',
    ],
    # 2. config_display_* -- ours only, upstream side empty.
    [
        '    <string name="config_display_auto_screen_carousel_secs_summary">Automatically toggles to the next page on the screen like a carousel, based the specified interval.</string>',
        '    <string name="config_display_compass_north_top_summary">The compass heading on the screen outside of the circle will always point north.</string>',
        '    <string name="config_display_displaymode_summary">Override default screen layout.</string>',
        '    <string name="config_display_flip_screen_summary">Flip screen vertically.</string>',
        '    <string name="config_display_heading_bold_summary">Bold the heading text on the screen.</string>',
        '    <string name="config_display_oled_summary">Override automatic OLED screen detection.</string>',
        '    <string name="config_display_screen_on_secs_summary">How long the screen remains on after the user button is pressed or messages are received.</string>',
        '    <string name="config_display_units_summary">Units displayed on the device screen.</string>',
        '    <string name="config_display_wake_on_tap_or_motion_summary">Requires that there be an accelerometer on your device.</string>',
        '    <string name="config_lora_frequency_slot_summary">Your node’s operating frequency is calculated based on the region, modem preset, and this field. When 0, the slot is automatically calculated based on the primary channel name and will change from the default public slot. Change back to the public default slot if private primary and public secondary channels are configured.</string>',
        '    <string name="config_lora_hop_limit_summary">Sets the maximum number of hops, default is 3. Increasing hops also increases congestion and should be used carefully. 0 hop broadcast messages will not get ACKs.</string>',
    ],
    # 3. distance_measurements_description/dns -- keep our deliberate "mt compatible" wording + dns.
    [
        '    <string name="distance_measurements_description">Display the distance between your phone and other mt compatible nodes with positions.</string>',
        '    <string name="dns">DNS</string>',
    ],
    # 4. gpio_pin_* -- ours only, upstream side empty.
    [
        '    <string name="gpio_pin_for_rotary_encoder_a_port">GPIO pin for rotary encoder A port</string>',
        '    <string name="gpio_pin_for_rotary_encoder_b_port">GPIO pin for rotary encoder B port</string>',
        '    <string name="gpio_pin_for_rotary_encoder_press_port">GPIO pin for rotary encoder Press port</string>',
        '    <string name="gpio_pin_to_monitor">GPIO pin to monitor</string>',
        '    <string name="gpio_read">Read</string>',
        '    <string name="gpio_read_result">GPIO read: %1$s</string>',
        '    <string name="gps_en_gpio">GPS EN GPIO</string>',
        '    <string name="gps_mode">GPS Mode (Physical Hardware)</string>',
        '    <string name="gps_receive_gpio">GPS Receive GPIO</string>',
        '    <string name="gps_transmit_gpio">GPS Transmit GPIO</string>',
    ],
    # 5. label_* -- keep all of ours, append upstream's new label_with_unit at the end (alphabetically after).
    [
        '    <!-- LABEL -->',
        '    <string name="label_lite_fast">Lite Fast</string>',
        '    <string name="label_lite_slow">Lite Slow</string>',
        '    <string name="label_long_fast">Long Fast</string>',
        '    <string name="label_long_moderate">Long Moderate</string>',
        '    <string name="label_long_slow">Long Slow</string>',
        '    <string name="label_long_turbo">Long Turbo</string>',
        '    <string name="label_medium_fast">Medium Fast</string>',
        '    <string name="label_medium_slow">Medium Slow</string>',
        '    <string name="label_medium_turbo">Medium Turbo</string>',
        '    <string name="label_narrow_fast">Narrow Fast</string>',
        '    <string name="label_narrow_slow">Narrow Slow</string>',
        '    <string name="label_short_fast">Short Fast</string>',
        '    <string name="label_short_slow">Short Slow</string>',
        '    <string name="label_short_turbo">Short Turbo</string>',
        '    <string name="label_tiny_fast">Tiny Fast</string>',
        '    <string name="label_tiny_slow">Tiny Slow</string>',
        '    <string name="label_very_long_slow">Very Long Slow</string>',
        '    <string name="label_with_unit">%1$s (%2$s)</string>',
    ],
    # 6. map_reporting_consent_text (encoding fix) -- keep our other map_reporting_* lines.
    [
        '    <string name="map_reporting_consent_text">By enabling this feature, you acknowledge and expressly consent to the transmission of your device’s real-time geographic location over the MQTT protocol without encryption. This location data may be used for purposes such as live map reporting, device tracking, and related telemetry functions.</string>',
        '    <string name="map_reporting_interval_seconds">Map reporting interval (seconds)</string>',
        '    <string name="map_reporting_summary">Your node will periodically send an unencrypted map report packet to the configured MQTT server, this includes id, long and short name, approximate location, hardware model, role, firmware version, LoRa region, modem preset and primary channel name.</string>',
    ],
    # 7. mqtt_status_reconnecting* (encoding fix) + new upstream subscription-refusal strings.
    [
        '    <string name="mqtt_status_reconnecting">Reconnecting…</string>',
        '    <string name="mqtt_status_reconnecting_with_attempt">Reconnecting (attempt %1$d) — %2$s</string>',
        '    <string name="mqtt_status_topics_refused_all">Connected, but the broker refused every topic: %1$s</string>',
        '    <string name="mqtt_status_topics_refused_some">Connected, but the broker refused %1$s</string>',
    ],
    # 8. on_demand_* -- ours only, upstream side empty. (49 lines, verbatim from HEAD)
    None,  # filled in below from the source block itself
    # 9. pt_BR (encoding fix) + ptt_pin ours-only.
    [
        '    <string name="pt_BR" translatable="false">Português do Brasil</string>',
        '    <string name="ptt_pin">PTT pin</string>',
    ],
    # 10. reconnecting (encoding fix) + red ours-only.
    [
        '    <string name="reconnecting">Reconnecting…</string>',
        '    <string name="red">Red</string>',
    ],
    # 11. save_* -- ours only, upstream side empty.
    [
        '    <string name="save_csv_in_storage_esp32_only">Save .CSV in storage (ESP32 only)</string>',
        '    <string name="save_log_to_file">Save log to file</string>',
    ],
    # 12. scanning_* (encoding fix) + screen_on_for ours-only.
    [
        '    <string name="scanning_bluetooth">Scanning…</string>',
        '    <string name="scanning_network">Scanning…</string>',
        '    <string name="screen_on_for">Screen on for</string>',
    ],
    # 13. shutdown_on_power_loss ours-only + shutdown_warning (encoding fix).
    [
        '    <string name="shutdown_on_power_loss">Shutdown on power loss</string>',
        '    <string name="shutdown_warning">⚠️ This will SHUTDOWN the node. Physical interaction will be required to turn it back on.</string>',
    ],
    # 14. smart_position/sniffer_* -- ours only, upstream side empty. (40 lines, verbatim from HEAD)
    None,  # filled in below from the source block itself
]

lines = content.split(CRLF)

resolved_count = 0
i = 0
out = []
res_idx = 0
while i < len(lines):
    if lines[i].strip() == "<<<<<<< HEAD":
        # find own-side end (=======) and upstream end (>>>>>>> upstream/main)
        sep = i + 1
        while lines[sep].strip() != "=======":
            sep += 1
        end = sep + 1
        while lines[end].strip() != ">>>>>>> upstream/main":
            end += 1
        ours_block = lines[i + 1 : sep]
        theirs_block = lines[sep + 1 : end]
        resolution = resolutions[res_idx]
        if resolution is None:
            # blocks 8 and 14: upstream side must be empty, take ours verbatim
            assert len(theirs_block) == 0, f"block {res_idx}: expected empty upstream side, got {theirs_block}"
            resolution = ours_block
        out.extend(resolution)
        res_idx += 1
        resolved_count += 1
        i = end + 1
    else:
        out.append(lines[i])
        i += 1

assert res_idx == len(resolutions), f"expected {len(resolutions)} conflicts, resolved {res_idx}"
print("resolved", resolved_count, "conflicts")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(CRLF.join(out))
print("EN strings.xml merge done, new line count:", len(out))
