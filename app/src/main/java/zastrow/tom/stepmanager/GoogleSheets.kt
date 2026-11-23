package zastrow.tom.stepmanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object GoogleSheets {

    // Replace with your actual Google Spreadsheet ID.
    private const val SPREADSHEET_ID = "1XxZbAe6jU8TJBVPor0KHkdWABVnBqFz6qKoNAt83pcE"

    // Replace with the actual name of your sheet tab.
    private const val SHEET_NAME = "Sheet1"

    suspend fun updateCell(
        accessToken: String,
        cell: String,
        value: Any
    ): Boolean = withContext(Dispatchers.IO) {

        val range = "'$SHEET_NAME'!$cell"
        val encodedRange = URLEncoder.encode(range, "UTF-8")

        val url = URL(
            "https://sheets.googleapis.com/v4/spreadsheets/" +
                    "$SPREADSHEET_ID/values/$encodedRange" +
                    "?valueInputOption=RAW"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "PUT"
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.doOutput = true

            val body = JSONObject().apply {
                put("values", JSONArray().put(
                    JSONArray().put(value)
                ))
            }

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray())
            }

            val responseCode = connection.responseCode

            if (responseCode in 200..299) {
                android.util.Log.d(
                    "GoogleSheetsApi",
                    "Successfully updated $range to $value"
                )
                true
            } else {
                val error = connection.errorStream
                    ?.bufferedReader()?.use { it.readText() }

                android.util.Log.e(
                    "GoogleSheetsApi",
                    "Update failed: HTTP $responseCode - $error"
                )
                false
            }
        } finally {
            connection.disconnect()
        }
    }
}
