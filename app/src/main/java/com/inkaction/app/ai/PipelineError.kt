package com.inkaction.app.ai

sealed class PipelineError(val message: String) {
    object MissingApiKey : PipelineError("Missing API key. Please open Settings to configure it.")
    object EmptyResponse : PipelineError("Received an empty response from AI.")
    class Network(reason: String) : PipelineError("Network error: $reason")
    class Unknown(reason: String) : PipelineError("Unknown error: $reason")
}
