package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class JogVelocityCapabilitiesTest {
    @Test public void absoluteSpeedCapabilityIsIndependentOfLegacyTempo() {
        DeviceState state=new DeviceState();
        state.accept(P2Codec.decode(P2Codec.encode(1,"MANUAL_JOG_VELOCITY_CAPABILITIES","5000")),100);
        assertEquals(5000,state.jogMaximumVelocityMdeg);
        state.accept(new P2Codec.Frame(2,"MANUAL_JOG_VELOCITY_CAPABILITIES",new String[]{"0"}),101);
        assertEquals(0,state.jogMaximumVelocityMdeg);
    }
    @Test public void panLimitIsIndependentAndAllowsFutureIncrease() {
        DeviceState state=new DeviceState();
        state.accept(new P2Codec.Frame(1,"MANUAL_JOG_VELOCITY_CAPABILITIES",new String[]{"5000"}),100);
        state.accept(P2Codec.decode(P2Codec.encode(2,"MANUAL_JOG_PAN_VELOCITY_CAPABILITIES","10000")),101);
        assertEquals(10000,state.jogMaximumPanVelocityMdeg); assertEquals(5000,state.jogMaximumVelocityMdeg);
        state.accept(new P2Codec.Frame(3,"MANUAL_JOG_PAN_VELOCITY_CAPABILITIES",new String[]{"12000"}),102);
        assertEquals(12000,state.jogMaximumPanVelocityMdeg);
    }
    @Test public void invalidCapabilityCannotChangeAcceptedLimit() {
        DeviceState state=new DeviceState();
        state.accept(new P2Codec.Frame(1,"MANUAL_JOG_VELOCITY_CAPABILITIES",new String[]{"5000"}),100);
        for(String value:new String[]{"-1","499","10001","NaN"}) {
            try { state.accept(new P2Codec.Frame(2,"MANUAL_JOG_VELOCITY_CAPABILITIES",new String[]{value}),101); fail("Invalid limit accepted"); }
            catch(IllegalArgumentException expected) { assertEquals(5000,state.jogMaximumVelocityMdeg); }
        }
    }
}
