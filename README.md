# pulsar-addon-migration-tool

A quick tool that take a filename which should contains a topics list of a addon pulsar formatted as:

```text
# /tmp/topics_list.txt
persistent://<tenant>/<namespace>/<topic>
persistent://<tenant>/<namespace>/<topic>
persistent://<tenant>/<namespace>/<topic>
persistent://<tenant>/<namespace>/<topic>
...
```

In the Tool.scala file, replace

ADDON_PULSAR_TOKEN_SOURCE by the token of the "old" addon.
ADDON_PULSAR_TOKEN_SINK by the token of the "new" addon.
ADDON_PULSAR_TENANT_SINK by the tenant of the "new" addon.
ADDON_PULSAR_NAMESPACE_SINK by the namespace of the "new" addon.

Then with configured source client and sink client. The tool will read all messages in each topic provided and produce them in the sink addon pulsar.

```bash
sbt compile
sbt run
```
