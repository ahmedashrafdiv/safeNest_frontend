package com.safenest.kids.security
enum class ProtectionMode { CONSUMER_UNMANAGED, DEVICE_ADMIN_ONLY, PROFILE_OWNER, DEVICE_OWNER, PROVISIONING_REQUIRED, TAMPERED_OR_UNKNOWN }
data class ProtectionStateSnapshot(val mode: ProtectionMode, val managementAuthorityConfirmed: Boolean, val uninstallProtectionConfirmed: Boolean, val lockTaskAvailable: Boolean, val stale: Boolean = false, val reason: String? = null)
object ProtectionStateDecider {
 fun decide(isDeviceOwner: Boolean,isProfileOwner: Boolean,isAdminActive: Boolean,uninstallBlocked: Boolean,lockTaskAvailable: Boolean,stale: Boolean): ProtectionStateSnapshot {
  if (stale) return ProtectionStateSnapshot(ProtectionMode.TAMPERED_OR_UNKNOWN,false,false,false,true,"management_state_stale")
  if (isDeviceOwner) return ProtectionStateSnapshot(ProtectionMode.DEVICE_OWNER,true,uninstallBlocked,lockTaskAvailable,false,if(uninstallBlocked)null else "uninstall_policy_not_confirmed")
  if (isProfileOwner) return ProtectionStateSnapshot(ProtectionMode.PROFILE_OWNER,true,uninstallBlocked,lockTaskAvailable,false,if(uninstallBlocked)null else "uninstall_policy_not_confirmed")
  if (isAdminActive) return ProtectionStateSnapshot(ProtectionMode.DEVICE_ADMIN_ONLY,false,false,false,false,"managed_owner_not_confirmed")
  return ProtectionStateSnapshot(ProtectionMode.CONSUMER_UNMANAGED,false,false,false,false,"managed_provisioning_required")
 }
}
