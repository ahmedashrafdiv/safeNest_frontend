package com.safenest.kids.security
enum class ProtectionHealth { HEALTHY, CONSUMER_BEST_EFFORT, PROVISIONING_REQUIRED, POLICY_NOT_CONFIRMED, MANAGEMENT_LOST, STALE }
object ProtectionHealthDecider {
 fun from(snapshot: ProtectionStateSnapshot): ProtectionHealth {
  if(snapshot.stale) return ProtectionHealth.STALE
  if(snapshot.mode == ProtectionMode.CONSUMER_UNMANAGED) return ProtectionHealth.CONSUMER_BEST_EFFORT
  if(snapshot.mode == ProtectionMode.PROVISIONING_REQUIRED) return ProtectionHealth.PROVISIONING_REQUIRED
  if(snapshot.mode == ProtectionMode.TAMPERED_OR_UNKNOWN) return ProtectionHealth.MANAGEMENT_LOST
  if(!snapshot.uninstallProtectionConfirmed) return ProtectionHealth.POLICY_NOT_CONFIRMED
  return ProtectionHealth.HEALTHY
 }
}
