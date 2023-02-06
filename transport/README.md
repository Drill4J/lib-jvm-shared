# Drill transport protocol

This protocol services for backend communications (agents <-> admin) 
Now it is some raw part of WS (https://tools.ietf.org/html/rfc6455)

port of https://libwebsockets.org/

#Development

https://libwebsockets.org/ - lws is the native lib for working with websockets.

This repository uses custom lib https://github.com/Drill4J/libwebsockets

websocket_bindings.def - file where TODO

## Local dev

Before start it needs to generate binding by gradle task

    gradle cinteropCommonWebsocket_bindingsLinuxX64

Publish

    gradle publishToMavenLocal    

