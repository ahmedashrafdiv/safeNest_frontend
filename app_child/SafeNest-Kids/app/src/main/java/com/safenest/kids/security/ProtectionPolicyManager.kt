package com.safenest.kids.security
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
data class ProtectionPolicyResult(val applied: Boolean,val reason: String?,val snapshot: ProtectionStateSnapshot)
object ProtectionPolicyManager {
 fun apply(context: Context): ProtectionPolicyResult {
  val dpm=context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
  val pkg=context.packageName
  val admin=ComponentName(context,LayngoDeviceAdminReceiver::class.java)
  val owner=dpm.isDeviceOwnerApp(pkg) || dpm.isProfileOwnerApp(pkg)
  if(!owner) return ProtectionPolicyResult(false,"managed_owner_not_confirmed",DeviceManagementHelper.read(context))
  return try {
   if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return ProtectionPolicyResult(false,"android_api_too_old",DeviceManagementHelper.read(context))
   dpm.setUninstallBlocked(admin,pkg,true)
   dpm.setLockTaskPackages(admin,arrayOf(pkg))
   val state=DeviceManagementHelper.read(context)
   if(state.uninstallProtectionConfirmed) ProtectionPolicyResult(true,null,state) else ProtectionPolicyResult(false,"uninstall_policy_not_confirmed",state)
  } catch(e: SecurityException) { ProtectionPolicyResult(false,"policy_permission_denied",DeviceManagementHelper.read(context)) }
 }
}
