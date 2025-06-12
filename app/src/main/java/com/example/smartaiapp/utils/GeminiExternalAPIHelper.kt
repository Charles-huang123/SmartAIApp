package com.example.smartaiapp.utils

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool

data class ExternalAPIParameter(
    val parameterName : String,
    val parameterDescription: String,
    val isRequired : Boolean = false
)

data class ExternalAPI(
    val methodName : String,
    val methodDescription: String,
    val listParameters : List<ExternalAPIParameter>
)

object GeminiExternalAPIHelper {
    private val _listExternalAPI = mutableListOf<FunctionDeclaration>()

    fun addExternalAPI(
        externalAPI:ExternalAPI
    ){
        _listExternalAPI.add(
            FunctionDeclaration(
                name = externalAPI.methodName,
                description = externalAPI.methodDescription,
                parameters = externalAPI.listParameters.associate {
                    it.parameterName to Schema.string(
                        description = it.parameterDescription,
                        nullable = it.isRequired
                    )
                }
            )
        )
    }

    fun getListExternalAPI() = listOf(
        Tool.functionDeclarations(
            _listExternalAPI.toList()
        ),
    )
}