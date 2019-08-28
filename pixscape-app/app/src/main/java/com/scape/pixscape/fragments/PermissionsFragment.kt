package com.scape.pixscape.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.scape.pixscape.R
import com.scape.scapekit.helper.PermissionHelper


/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 */
internal class PermissionsFragment : Fragment() {

    private fun hasPermissions() = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if required permissions have been granted and prompt the user to grant the ones that haven't been granted yet.
     */
    private fun checkAndRequestPermissions() {
        val deniedPermissions = PermissionHelper.checkPermissions(requireContext())

        if (deniedPermissions.isEmpty()) {
            Navigation.findNavController(requireActivity(), R.id.fragment_main).navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        } else {
            PermissionHelper.requestPermissions(this, deniedPermissions)
        }
    }

    private fun startSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", activity?.packageName, null)
        this.startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasPermissions()) {
            // Request permissions
            checkAndRequestPermissions()
        } else {
            // If permissions have already been granted, proceed
            Navigation.findNavController(requireActivity(), R.id.fragment_main).navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val deniedPermissions = PermissionHelper.checkPermissions(requireContext())
        if (deniedPermissions.isEmpty()) {
            Navigation.findNavController(requireActivity(), R.id.fragment_main).navigate(PermissionsFragmentDirections.actionPermissionsToCamera())
        }
        else {
            val errorMessage: String = when {
                deniedPermissions.contains(Manifest.permission.CAMERA) -> getString(R.string.permission_missing_camera)
                deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) -> getString(R.string.permission_missing_location)
                deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> getString(R.string.permission_missing_storage)
                else -> "Missing permissions"
            }

            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG)

            val dialog = AlertDialog.Builder(requireContext())
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> startSettings() }
                    .create()
            dialog.show()
        }
    }

    companion object {
        private val PERMISSIONS_REQUIRED = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
