package com.safenest.kids.security
import org.junit.Assert.assertEquals
import org.junit.Test
class ProtectionHealthTest {
 @Test fun consumerIsBestEffort(){ val s=ProtectionStateDecider.decide(false,false,false,false,false,false); assertEquals(ProtectionHealth.CONSUMER_BEST_EFFORT,ProtectionHealthDecider.from(s)) }
 @Test fun unconfirmedPolicyIsNotHealthy(){ val s=ProtectionStateDecider.decide(true,false,true,false,true,false); assertEquals(ProtectionHealth.POLICY_NOT_CONFIRMED,ProtectionHealthDecider.from(s)) }
 @Test fun staleStateIsStale(){ val s=ProtectionStateDecider.decide(true,false,true,true,true,true); assertEquals(ProtectionHealth.STALE,ProtectionHealthDecider.from(s)) }
}
