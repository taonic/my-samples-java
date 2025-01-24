# Context-Aware Payload Encryption in Temporal

This repository demonstrates context-aware payload encryption in Temporal workflows, where encryption keys are determined dynamically based on the activity type.

## How It Works

### Context-Aware Encryption Key Selection
1.	Activity Type Awareness:
- The custom codec (CryptCodec) determines the encryption key based on the activity type using the SerializationContext.
- For example:
  - If the activity type is ComposeGreeting, a specific key (compose-greeting-key) is used.
  - For all other activities, a generic key is applied.
2.	Dynamic Key Assignment:
- Method getKeyId() in the codec checks the activity type and assigns the appropriate key:
```
private String getKeyId() {
  if (Objects.equals(this.getActivityType(), "ComposeGreeting")) {
    return "compose-greeting-key-test-------";
  }
  return "generic-encryption-key-test-----";
}
```

### Getting Started

#### Prerequisites
- Java 8 or later
- Temporal Java SDK
- Temporal Dev Server

#### Steps to Run

Start a dev server:
```
temporal server start-dev
```

Run the sample:
```
./gradlew -q execute -PmainClass=io.temporal.samples.contextAwareCodec.Starter
```

Check the input payload's `encryption-key-id` key. Different codec key (base64 encoded) should be used for Workflow's input and ComposeGreeting Activity's input.