package ru.kontur.cdp4k.protocol.inspector

import ru.kontur.cdp4k.protocol.CdpEvent
import ru.kontur.cdp4k.protocol.CdpExperimental

@CdpExperimental
abstract class InspectorEvent internal constructor() : CdpEvent()
