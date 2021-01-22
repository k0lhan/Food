/*
 * Created by Evgeniya Zemlyanaya (@zzemlyanaya)
 * Copyright (c) 2021 . All rights reserved.
 * Last modified 21.01.2021, 19:40
 */

package ru.zzemlyanaya.tfood.model

data class ErrorResponse (
        var statusCode: Int,
        var message: String
        )