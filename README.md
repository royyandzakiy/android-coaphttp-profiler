# CoAP HTTP Profiling

## An app to profile the performance of CoAP Protocol and HTTP Protocol

this project is based on the [nCoap](https://github.com/okleine/nCoAP) library and [spitfirefox project] (https://github.com/okleine/spitfirefox)

metrics to measure:
- Total Request (N)
- Total Time for N Request (ms)
- Packet Loss (packets)
- Avg/Max/Min (ms)
- Response Time per Request (ms)

hardcoded settings:
```
    serverName = "ec2-54-169-136-164.ap-southeast-1.compute.amazonaws.com"
    portNumber = 5683
    localUri = ""
    confirmable = true
    observe = false
    acceptedFormats = ""
    payloadFormat = "50"
    payload = "{\"id\":"+idCoapRequest+"}"
    ifMatch = ""
    etags = ""
```

---

used for research in the eFishery Internship Programme