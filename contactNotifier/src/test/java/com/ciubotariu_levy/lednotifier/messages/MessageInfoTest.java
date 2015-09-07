package com.ciubotariu_levy.lednotifier.messages;


import android.graphics.Color;

import com.ciubotariu_levy.lednotifier.BuildConfig;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class MessageInfoTest {
    private static final String NAME = "name";
    private static final String ADDRESS = "address";
    private static final String RINGTONE = "content://ringtone";
    private static final String CONTACT_URI = "content://contacts/a";
    private static final String VIB_PATTERN = "0,10,10,10";
    private static final String CONTENT_STRING_1 = "A";
    private static final String CONTENT_STRING_2 = "B";
    private static final String CONTENT_STRING_3 = "C";

    private static final int CUSTOM_COLOR = Color.RED;
    private static final int NON_CUSTOM_COLOR = Color.GRAY;

    @Test
    public void storesGivenName() {
        MessageInfo info = new MessageInfo();
        info.setName(NAME);
        Assert.assertEquals(NAME, info.getName());
    }

    @Test
    public void storesGivenAddress() {
        MessageInfo info = new MessageInfo();
        info.setAddress(ADDRESS);
        Assert.assertEquals(ADDRESS, info.getAddress());
    }

    @Test
    public void storesGivenRingtone() {
        MessageInfo info = new MessageInfo();
        info.setRingtoneUriString(RINGTONE);
        Assert.assertEquals(RINGTONE, info.getRingtoneUriString());
    }

    @Test
    public void storesGivenVibPattern() {
        MessageInfo info = new MessageInfo();
        info.setVibPattern(VIB_PATTERN);
        Assert.assertEquals(VIB_PATTERN, info.getVibPattern());
    }

    @Test
    public void storesGivenColor() {
        MessageInfo info = new MessageInfo();
        info.setColor(CUSTOM_COLOR);
        Assert.assertEquals(CUSTOM_COLOR, info.getColor());
    }

    @Test
    public void storesGivenContactUriString() {
        MessageInfo info = new MessageInfo();
        info.setContactUriString(CONTACT_URI);
        Assert.assertEquals(CONTACT_URI, info.getContactUriString());
    }

    @Test
    public void storesGivenContentString() {
        MessageInfo info = new MessageInfo();
        info.addContentString(CONTENT_STRING_1);
        Assert.assertEquals(CONTENT_STRING_1, info.getContentString());
    }

    @Test
    public void concatsMultipleContentStringsWithNewlines() {
        MessageInfo info = new MessageInfo();
        info.addContentString(CONTENT_STRING_1);
        info.addContentString(CONTENT_STRING_2);
        info.addContentString(CONTENT_STRING_3);
        Assert.assertEquals(CONTENT_STRING_1 + "\n" + CONTENT_STRING_2 + "\n" + CONTENT_STRING_3, info.getContentString());
    }

    @Test
    public void getNameOrAddress_returnsNameIfNonNull() {
        MessageInfo info = new MessageInfo();
        info.setName(NAME);
        info.setAddress(ADDRESS);
        Assert.assertEquals(NAME, info.getNameOrAddress());
    }

    @Test
    public void getNameOrAddress_returnsAddressIfNameNullAndAddressNonNull() {
        MessageInfo info = new MessageInfo();
        info.setAddress(ADDRESS);
        Assert.assertEquals(ADDRESS, info.getNameOrAddress());
    }

    @Test
    public void getNameOrAddress_returnsUnknownIfNameAndAddressNull() {
        MessageInfo info = new MessageInfo();
        Assert.assertEquals(MessageInfo.UNKNOWN_SENDER, info.getNameOrAddress());
    }

    @Test
    public void hasCustomColor_returnsTrueForCustomColor() {
        MessageInfo info = new MessageInfo();
        info.setColor(CUSTOM_COLOR);
        Assert.assertTrue(info.hasCustomColor());
    }

    @Test
    public void hasCustomColor_returnsFalseForNoCustomColor() {
        MessageInfo info = new MessageInfo();
        Assert.assertFalse(info.hasCustomColor());
        info.setColor(NON_CUSTOM_COLOR);
        Assert.assertFalse(info.hasCustomColor());
    }

    @Test
    public void hasCustomRing_returnsTrueWhenRingtoneNonNull() {
        MessageInfo info = new MessageInfo();
        info.setRingtoneUriString(RINGTONE);
        Assert.assertTrue(info.hasCustomRing());
    }

    @Test
    public void hasCustomRing_returnsFalseWhenRingoneNull() {
        MessageInfo info = new MessageInfo();
        Assert.assertFalse(info.hasCustomRing());
    }

    @Test
    public void hasCustomVib_returnsTrueWhenNotEmpty() {
        MessageInfo info = new MessageInfo();
        info.setVibPattern(VIB_PATTERN);
        Assert.assertTrue(info.hasCustomVib());
    }

    @Test
    public void hasCustomVib_returnsFalseWhenNull() {
        MessageInfo info = new MessageInfo();
        Assert.assertFalse(info.hasCustomVib());
    }

    @Test
    public void hasCustomVib_returnsFalseWhenEmptyString() {
        MessageInfo info = new MessageInfo();
        info.setVibPattern("");
        Assert.assertFalse(info.hasCustomVib());
    }

    @Test
    public void isCustom_returnsFalseWhenContactUriStringNull() {
        MessageInfo info = new MessageInfo();
        info.setRingtoneUriString(RINGTONE);
        Assert.assertFalse(info.isCustom());
    }

    @Test
    public void isCustom_returnsTrueWhenContactUriNonNullAndCustomColor() {
        MessageInfo info = new MessageInfo();
        info.setContactUriString(CONTACT_URI);
        info.setColor(CUSTOM_COLOR);
        Assert.assertTrue(info.isCustom());
    }

    @Test
    public void isCustom_returnsTrueWhenContactUriNonNullAndCustomRing() {
        MessageInfo info = new MessageInfo();
        info.setContactUriString(CONTACT_URI);
        info.setRingtoneUriString(RINGTONE);
        Assert.assertTrue(info.isCustom());
    }

    @Test
    public void isCustom_returnsTrueWhenContactUriNonNullAndCustomVib() {
        MessageInfo info = new MessageInfo();
        info.setContactUriString(CONTACT_URI);
        info.setVibPattern(VIB_PATTERN);
        Assert.assertTrue(info.isCustom());
    }
}