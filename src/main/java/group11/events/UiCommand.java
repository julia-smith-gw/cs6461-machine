package group11.events;


// UI -> Model (intents/commands)
public sealed interface UiCommand permits SetGPR, SetIXR, SetPC, SetMAR, SetMBR, SetConsoleInput {}



