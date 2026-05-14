package com.example.janaushadhifinder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

object MedicineRepository {
    suspend fun loadMedicines(context: Context): List<Medicine> = withContext(Dispatchers.IO) {
        val medicines = mutableListOf<Medicine>()
        try {
            context.assets.open("medicines.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val jsonString = reader.readText()
                val jsonArray = JSONArray(jsonString)
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    medicines.add(
                        Medicine(
                            name = jsonObj.getString("name"),
                            generic = jsonObj.getString("generic"),
                            brandedPrice = jsonObj.getInt("brandedPrice"),
                            genericPrice = jsonObj.getInt("genericPrice"),
                            category = jsonObj.getString("category")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext medicines
    }
}
