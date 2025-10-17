package group11.events;


// Model -> UI (state changes)
public sealed interface CPUEvent permits GPRChanged, IXRChanged, PCChanged, MARChanged, MBRChanged, IRChanged, MessageChanged {}
