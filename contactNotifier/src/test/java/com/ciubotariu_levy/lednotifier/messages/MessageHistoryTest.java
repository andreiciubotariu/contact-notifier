package com.ciubotariu_levy.lednotifier.messages;

import android.graphics.Color;
import android.os.Message;

import com.ciubotariu_levy.lednotifier.BuildConfig;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.LinkedHashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class MessageHistoryTest {
    private static final String MESSAGE_INFO_ADDRESS = "address";

    private void addMessageToHistory(String address, MessageHistory history) {
        MessageInfo info = new MessageInfo();
        info.setAddress(address);
        LinkedHashMap<String, MessageInfo> messageInfoLinkedHashMap = new LinkedHashMap<>();
        messageInfoLinkedHashMap.put(info.getAddress(), info);
        history.addNewMessages(messageInfoLinkedHashMap);
    }

    @Test
    public void addNewMessages_addsMessages() {
        final String MESSAGE_INFO_ADDRESS_1 = "address_1";
        final String MESSAGE_INFO_ADDRESS_2 = "address_2";
        MessageHistory messageHistory = new MessageHistory();
        addMessageToHistory(MESSAGE_INFO_ADDRESS_1, messageHistory);
        addMessageToHistory(MESSAGE_INFO_ADDRESS_2, messageHistory);

        Assert.assertEquals(2, messageHistory.getMessages().size());
        Assert.assertFalse(messageHistory.containsCustomMessages());
        Assert.assertNotNull(messageHistory.getMessages().get(MESSAGE_INFO_ADDRESS_1));
        Assert.assertNotNull(messageHistory.getMessages().get(MESSAGE_INFO_ADDRESS_2));
    }

    @Test
    public void addNewMessages_onlyOneMessageForSameAddress() {
        MessageHistory messageHistory = new MessageHistory();
        LinkedHashMap<String, MessageInfo> messageInfoLinkedHashMap = new LinkedHashMap<>();
        MessageInfo info = new MessageInfo();
        info.setAddress(MESSAGE_INFO_ADDRESS);
        messageInfoLinkedHashMap.put(info.getAddress(), info);
        messageHistory.addNewMessages(messageInfoLinkedHashMap);
        messageInfoLinkedHashMap = new LinkedHashMap<>();
        info = new MessageInfo();
        info.setAddress(MESSAGE_INFO_ADDRESS);
        messageInfoLinkedHashMap.put(info.getAddress(), info);
        messageHistory.addNewMessages(messageInfoLinkedHashMap);

        Assert.assertEquals(1, messageHistory.getMessages().size());
        Assert.assertFalse(messageHistory.containsCustomMessages());
        Assert.assertEquals(info.getAddress(), messageHistory.getMessages().get(info.getAddress()).getAddress());
    }

    @Test
    public void setNotificationMessages_setsCustomRingtoneMessage() {
        final String CUSTOM_RINGTONE = "custom_ringtone";

        MessageHistory messageHistory = new MessageHistory();
        LinkedHashMap<String, MessageInfo> messageInfoLinkedHashMap = new LinkedHashMap<>();
        MessageInfo info = mock(MessageInfo.class);
        when(info.getAddress()).thenReturn(MESSAGE_INFO_ADDRESS);
        when(info.isCustom()).thenReturn(true);
        when(info.hasCustomRing()).thenReturn(true);
        when(info.getRingtoneUriString()).thenReturn(CUSTOM_RINGTONE);
        messageInfoLinkedHashMap.put(info.getAddress(), info);
        messageHistory.addMessages(messageInfoLinkedHashMap);
        messageHistory.setNotificationMessages();

        Assert.assertEquals(1, messageHistory.getMessages().size());
        Assert.assertTrue(messageHistory.containsCustomMessages());
        Assert.assertEquals(CUSTOM_RINGTONE, messageHistory.getCustomRingtone());
    }

    @Test
    public void setNotificationMessages_setsCustomVibPatternMessage() {
        final String CUSTOM_VIB = "0,10,10,10";

        MessageHistory messageHistory = new MessageHistory();
        LinkedHashMap<String, MessageInfo> messageInfoLinkedHashMap = new LinkedHashMap<>();
        MessageInfo info = mock(MessageInfo.class);
        when(info.getAddress()).thenReturn(MESSAGE_INFO_ADDRESS);
        when(info.isCustom()).thenReturn(true);
        when(info.hasCustomVib()).thenReturn(true);
        when(info.getVibPattern()).thenReturn(CUSTOM_VIB);
        messageInfoLinkedHashMap.put(info.getAddress(), info);
        messageHistory.addMessages(messageInfoLinkedHashMap);
        messageHistory.setNotificationMessages();

        Assert.assertEquals(1, messageHistory.getMessages().size());
        Assert.assertTrue(messageHistory.containsCustomMessages());
        Assert.assertEquals(CUSTOM_VIB, messageHistory.getCustomVibPattern());
    }

    @Test
    public void setNotificationMessages_setsCustomColorMessage() {
        final String MESSAGE_INFO_ADDRESS = "addresss";
        final int CUSTOM_COLOR = Color.RED;

        MessageHistory messageHistory = new MessageHistory();
        LinkedHashMap<String, MessageInfo> messageInfoLinkedHashMap = new LinkedHashMap<>();
        MessageInfo info = mock(MessageInfo.class);
        when(info.getAddress()).thenReturn(MESSAGE_INFO_ADDRESS);
        when(info.isCustom()).thenReturn(true);
        when(info.hasCustomColor()).thenReturn(true);
        when(info.getColor()).thenReturn(CUSTOM_COLOR);
        messageInfoLinkedHashMap.put(info.getAddress(), info);
        messageHistory.addMessages(messageInfoLinkedHashMap);
        messageHistory.setNotificationMessages();

        Assert.assertEquals(1, messageHistory.getMessages().size());
        Assert.assertTrue(messageHistory.containsCustomMessages());
        Assert.assertEquals(CUSTOM_COLOR, messageHistory.getCustomColor());
    }
}