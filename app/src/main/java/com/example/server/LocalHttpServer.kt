package com.example.server

import android.content.Context
import com.example.data.database.InboxItem
import com.example.data.domain.InboxRepository
import com.example.device.DeviceInfoProvider
import com.example.device.StorageInfoProvider
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID

class LocalHttpServer(
    port: Int,
    private val context: Context,
    private val repository: InboxRepository,
    private var activePin: String
) : NanoHTTPD(port) {

    private val sessionTokens = mutableSetOf<String>()
    private val deviceInfoProvider = DeviceInfoProvider(context)
    private val storageInfoProvider = StorageInfoProvider(context)

    fun updatePin(newPin: String) {
        activePin = newPin
        sessionTokens.clear()
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (uri == "/" || uri == "/index.html") {
            return newFixedLengthResponse(Response.Status.OK, "text/html", WebAssets.INDEX_HTML)
        }

        if (uri == "/api/login" && method == Method.POST) {
            val body = parseJsonBody(session)
            val pin = body.optString("pin")
            return if (pin == activePin) {
                val token = UUID.randomUUID().toString()
                sessionTokens.add(token)
                jsonResponse(JSONObject().put("status", "ok").put("token", token))
            } else {
                jsonResponse(JSONObject().put("status", "error").put("message", "Invalid PIN"), Response.Status.UNAUTHORIZED)
            }
        }

        // Authenticate API requests
        val token = session.headers["x-auth-token"] ?: getQueryParam(session, "token")
        if (token == null || !sessionTokens.contains(token)) {
            return jsonResponse(JSONObject().put("error", "Unauthorized"), Response.Status.UNAUTHORIZED)
        }

        return try {
            when {
                uri == "/api/status" -> jsonResponse(JSONObject().put("status", "running"))
                uri == "/api/device" -> handleDevice()
                uri == "/api/storage" -> handleStorage()
                uri == "/api/items" && method == Method.GET -> handleGetItems(session)
                uri.startsWith("/api/files/") && method == Method.GET -> handleServeFile(session, uri)
                uri == "/api/items/text" && method == Method.POST -> handleCreateText(session)
                uri == "/api/items/url" && method == Method.POST -> handleCreateUrl(session)
                uri == "/api/upload" && method == Method.POST -> handleUpload(session)
                uri.startsWith("/api/items/") && method == Method.DELETE -> handleDeleteItem(uri)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Not Found\"}")
            }
        } catch (e: Exception) {
            jsonResponse(JSONObject().put("error", e.message ?: "Server error"), Response.Status.INTERNAL_ERROR)
        }
    }

    private fun handleDevice(): Response {
        val d = deviceInfoProvider.getDeviceDetails()
        val json = JSONObject()
            .put("manufacturer", d.manufacturer)
            .put("model", d.model)
            .put("androidVersion", d.androidVersion)
            .put("batteryPercent", d.batteryPercent)
            .put("isCharging", d.isCharging)
            .put("ipAddress", d.ipAddress)
        return jsonResponse(json)
    }

    private fun handleStorage(): Response {
        val s = storageInfoProvider.getSystemStorageStats()
        val totalInboxSize = runBlocking { repository.getTotalInboxSize().first() ?: 0L }
        val json = JSONObject()
            .put("totalBytes", s.totalBytes)
            .put("usedBytes", s.usedBytes)
            .put("freeBytes", s.freeBytes)
            .put("inboxSize", totalInboxSize)
        return jsonResponse(json)
    }

    private fun handleGetItems(session: IHTTPSession): Response {
        val query = getQueryParam(session, "q") ?: ""
        val typeFilter = getQueryParam(session, "type")

        val items = runBlocking {
            if (query.isNotBlank() || typeFilter != null) {
                repository.searchItems(query, typeFilter).first()
            } else {
                repository.getAllItems().first()
            }
        }

        val jsonArray = JSONArray()
        items.forEach { item ->
            jsonArray.put(itemToJson(item))
        }
        return jsonResponse(jsonArray)
    }

    private fun handleServeFile(session: IHTTPSession, uri: String): Response {
        val idStr = uri.removePrefix("/api/files/").split("?")[0]
        val id = idStr.toLongOrNull() ?: return jsonResponse(JSONObject().put("error", "Invalid ID"), Response.Status.BAD_REQUEST)

        val item = runBlocking { repository.getItemById(id) }
        if (item?.localFilePath == null) {
            return jsonResponse(JSONObject().put("error", "File not found"), Response.Status.NOT_FOUND)
        }

        val file = File(item.localFilePath)
        if (!file.exists()) {
            return jsonResponse(JSONObject().put("error", "Physical file missing"), Response.Status.NOT_FOUND)
        }

        val mimeType = item.mimeType
        val isDownload = getQueryParam(session, "download") == "true"

        // Handle HTTP Range for media seeking
        val rangeHeader = session.headers["range"]
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            return serveRangeFile(file, mimeType, rangeHeader)
        }

        val response = newFixedLengthResponse(Response.Status.OK, mimeType, FileInputStream(file), file.length())
        if (isDownload) {
            response.addHeader("Content-Disposition", "attachment; filename=\"${item.originalFileName ?: file.name}\"")
        }
        return response
    }

    private fun serveRangeFile(file: File, mimeType: String, rangeHeader: String): Response {
        val fileLength = file.length()
        var start: Long = 0
        var end: Long = fileLength - 1

        val range = rangeHeader.removePrefix("bytes=").split("-")
        try {
            if (range[0].isNotEmpty()) start = range[0].toLong()
            if (range.size > 1 && range[1].isNotEmpty()) end = range[1].toLong()
        } catch (e: Exception) { }

        if (end >= fileLength) end = fileLength - 1
        val contentLength = end - start + 1

        val fis = FileInputStream(file)
        fis.skip(start)

        val response = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, fis, contentLength)
        response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
        response.addHeader("Content-Length", contentLength.toString())
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    private fun handleCreateText(session: IHTTPSession): Response {
        val body = parseJsonBody(session)
        val text = body.optString("text")
        if (text.isBlank()) return jsonResponse(JSONObject().put("error", "Empty text"), Response.Status.BAD_REQUEST)

        val item = runBlocking { repository.saveTextOrUrl(text, sourceApp = "Web Access") }
        return jsonResponse(itemToJson(item))
    }

    private fun handleCreateUrl(session: IHTTPSession): Response {
        val body = parseJsonBody(session)
        val url = body.optString("url")
        if (url.isBlank()) return jsonResponse(JSONObject().put("error", "Empty URL"), Response.Status.BAD_REQUEST)

        val item = runBlocking { repository.saveTextOrUrl(url, sourceApp = "Web Access") }
        return jsonResponse(itemToJson(item))
    }

    private fun handleUpload(session: IHTTPSession): Response {
        val filesMap = HashMap<String, String>()
        session.parseBody(filesMap)

        var uploadedCount = 0
        for ((key, tempFilePath) in filesMap) {
            if (key.startsWith("file") || key == "content") {
                val tempFile = File(tempFilePath)
                if (tempFile.exists()) {
                    val originalName = session.parameters[key]?.firstOrNull() ?: tempFile.name
                    val mimeType = session.headers["content-type"] ?: "application/octet-stream"

                    runBlocking {
                        FileInputStream(tempFile).use { inputStream ->
                            repository.saveInputStream(inputStream, originalName, mimeType, sourceApp = "Web Upload")
                        }
                    }
                    uploadedCount++
                }
            }
        }
        return jsonResponse(JSONObject().put("status", "ok").put("count", uploadedCount))
    }

    private fun handleDeleteItem(uri: String): Response {
        val idStr = uri.removePrefix("/api/items/").split("?")[0]
        val id = idStr.toLongOrNull() ?: return jsonResponse(JSONObject().put("error", "Invalid ID"), Response.Status.BAD_REQUEST)

        runBlocking {
            val item = repository.getItemById(id)
            if (item != null) {
                repository.deleteItem(item)
            }
        }
        return jsonResponse(JSONObject().put("status", "deleted"))
    }

    private fun parseJsonBody(session: IHTTPSession): JSONObject {
        val map = HashMap<String, String>()
        session.parseBody(map)
        val postData = map["postData"] ?: ""
        return try {
            JSONObject(postData)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    private fun getQueryParam(session: IHTTPSession, name: String): String? {
        return session.parameters[name]?.firstOrNull()
    }

    private fun itemToJson(item: InboxItem): JSONObject {
        return JSONObject()
            .put("id", item.id)
            .put("type", item.type)
            .put("title", item.title)
            .put("text", item.text)
            .put("url", item.url)
            .put("localFilePath", item.localFilePath)
            .put("mimeType", item.mimeType)
            .put("originalFileName", item.originalFileName)
            .put("fileSize", item.fileSize)
            .put("createdAt", item.createdAt)
            .put("favorite", item.favorite)
            .put("archived", item.archived)
    }

    private fun jsonResponse(data: Any, status: Response.IStatus = Response.Status.OK): Response {
        val response = newFixedLengthResponse(status, "application/json", data.toString())
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }
}
