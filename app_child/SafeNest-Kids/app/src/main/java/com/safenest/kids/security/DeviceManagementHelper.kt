package com.safenest.kids.security
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
object DeviceManagementHelper {
 fun read(context: Context): ProtectionStateSnapshot {
  val dpm=context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
  val pkg=context.packageName
  val admin=ComponentName(context,LayngoDeviceAdminReceiver::class.java)
  val adminActive=dpm.isAdminActive(admin)
  val deviceOwner=dpm.isDeviceOwnerApp(pkg)
  val profileOwner=dpm.isProfileOwnerApp(pkg)
  val uninstallBlocked=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N && (deviceOwner || profileOwner)) dpm.isUninstallBlocked(admin,pkg) else false
  val lockTask=if(deviceOwner || profileOwner) dpm.isLockTaskPermitted(pkg) else false
  return ProtectionStateDecider.decide(deviceOwner,profileOwner,adminActive,uninstallBlocked,lockTask,false)
 }
}
