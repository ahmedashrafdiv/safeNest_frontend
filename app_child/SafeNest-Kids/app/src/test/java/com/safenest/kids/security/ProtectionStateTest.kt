package com.safenest.kids.security
import org.junit.Assert.*
import org.junit.Test
class ProtectionStateTest {
 @Test fun consumerDoesNotClaimProtection(){ val s=ProtectionStateDecider.decide(false,false,false,false,false,false); assertEquals(ProtectionMode.CONSUMER_UNMANAGED,s.mode); assertFalse(s.uninstallProtectionConfirmed) }
 @Test fun ownerNeedsConfirmedUninstallPolicy(){ val s=ProtectionStateDecider.decide(true,false,true,false,true,false); assertEquals(ProtectionMode.DEVICE_OWNER,s.mode); assertFalse(s.uninstallProtectionConfirmed) }
 @Test fun profileOwnerCanBeProtected(){ val s=ProtectionStateDecider.decide(false,true,true,true,true,false); assertEquals(ProtectionMode.PROFILE_OWNER,s.mode); assertTrue(s.managementAuthorityConfirmed); assertTrue(s.uninstallProtectionConfirmed) }
 @Test fun staleFailsClosed(){ val s=ProtectionStateDecider.decide(true,false,true,true,true,true); assertEquals(ProtectionMode.TAMPERED_OR_UNKNOWN,s.mode); assertFalse(s.managementAuthorityConfirmed); assertFalse(s.uninstallProtectionConfirmed) }
}
