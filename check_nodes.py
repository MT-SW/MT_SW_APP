import sqlite3
conn = sqlite3.connect("local_copy.db")
cur = conn.execute("SELECT num, long_name, short_name FROM nodes WHERE long_name LIKE '%SM_Mobile%' OR long_name LIKE '%Lysica%' OR long_name LIKE '%SQ7DAR%' OR short_name LIKE '%SQ7%'")
rows = cur.fetchall()
for r in rows:
    print(r)
    hist = conn.execute("SELECT COUNT(*), MIN(timestamp), MAX(timestamp) FROM node_metrics_history WHERE num = ?", (r[0],)).fetchone()
    print("  history:", hist)
    sample = conn.execute("SELECT timestamp, battery_level, snr, rssi, channel_utilization, air_util_tx, temperature FROM node_metrics_history WHERE num = ? ORDER BY timestamp DESC LIMIT 5", (r[0],)).fetchall()
    for s in sample:
        print("   ", s)
