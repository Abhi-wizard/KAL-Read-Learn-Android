package com.example.kal.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A repository for handling all Firebase Storage operations,
 * like uploading images.
 *
 * We inject FirebaseStorage which is provided by the FirebaseModule.
 */
@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {

    /**
     * Uploads an image to a specified folder in Firebase Storage and returns
     * the public download URL.
     *
     * @param imageUri The local URI of the image to upload.
     * @param folder The folder in Firebase Storage to upload to (e.g., "post_covers").
     * @return A [Result] object containing the download URL String on success,
     * or an Exception on failure.
     */
    suspend fun uploadImage(
        imageUri: Uri,
        folder: String
    ): Result<String> {
        return try {
            // Create a unique file name for the image
            val fileName = UUID.randomUUID().toString()

            // Get a reference to the storage path: e.g., "post_covers/unique-id-123.jpg"
            val storageRef = storage.reference.child("$folder/$fileName")

            // 1. Upload the file
            storageRef.putFile(imageUri).await()

            // 2. Get the public download URL for the file
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 3. Return the URL successfully
            Result.success(downloadUrl)

        } catch (e: Exception) {
            // Handle any exceptions (e.g., no internet, permissions)
            Result.failure(e)
        }
    }
}