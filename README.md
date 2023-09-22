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

Then with configured source client and sink client. The tool will read all messages in each topic provided and produce them in the sink addon pulsar.

```bash
sbt compile
sbt run
```
