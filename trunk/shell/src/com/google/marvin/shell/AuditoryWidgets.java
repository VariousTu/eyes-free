package com.google.marvin.shell;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.google.tts.TTS;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

public class AuditoryWidgets {
  private TTS tts;
  private MarvinShell parent;
  private final ReentrantLock speakingTimeLock = new ReentrantLock();

  public AuditoryWidgets(TTS theTts, MarvinShell shell) {
    tts = theTts;
    parent = shell;
  }

  public void announceBattery() {
    BroadcastReceiver battReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        context.unregisterReceiver(this);
        int rawlevel = intent.getIntExtra("level", -1);
        int scale = intent.getIntExtra("scale", -1);
        String state = intent.getStringExtra("state");
        int status = intent.getIntExtra("status", -1);
        if (rawlevel >= 0 && scale > 0) {
          int batteryLevel = (rawlevel * 100) / scale;
          tts.speak(Integer.toString(batteryLevel), 0, null);
          tts.speak("%", 1, null);
        }
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
          tts.speak("[slnc]", 1, null);
          tts.speak(parent.getString(R.string.charging), 1, null);
        }
      }
    };
    IntentFilter battFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    parent.registerReceiver(battReceiver, battFilter);
  }

  public void announceDate() {
    GregorianCalendar cal = new GregorianCalendar();
    int month = cal.get(Calendar.MONTH);
    int day = cal.get(Calendar.DAY_OF_MONTH);
    int year = cal.get(Calendar.YEAR);
    String monthStr = "";
    switch (month) {
      case Calendar.JANUARY:
        monthStr = parent.getString(R.string.january);
        break;
      case Calendar.FEBRUARY:
        monthStr = parent.getString(R.string.february);
        break;
      case Calendar.MARCH:
        monthStr = parent.getString(R.string.march);
        break;
      case Calendar.APRIL:
        monthStr = parent.getString(R.string.april);
        break;
      case Calendar.MAY:
        monthStr = parent.getString(R.string.may);
        break;
      case Calendar.JUNE:
        monthStr = parent.getString(R.string.june);
        break;
      case Calendar.JULY:
        monthStr = parent.getString(R.string.july);
        break;
      case Calendar.AUGUST:
        monthStr = parent.getString(R.string.august);
        break;
      case Calendar.SEPTEMBER:
        monthStr = parent.getString(R.string.september);
        break;
      case Calendar.OCTOBER:
        monthStr = parent.getString(R.string.october);
        break;
      case Calendar.NOVEMBER:
        monthStr = parent.getString(R.string.november);
        break;
      case Calendar.DECEMBER:
        monthStr = parent.getString(R.string.december);
        break;
    }
    try {
      boolean canSpeak = speakingTimeLock.tryLock(1000, TimeUnit.MILLISECONDS);
      if (canSpeak) {
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak(monthStr, 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak(Integer.toString(day), 1, null);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      speakingTimeLock.unlock();
    }
  }

  public void announceTime() {
    GregorianCalendar cal = new GregorianCalendar();
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int minutes = cal.get(Calendar.MINUTE);
    String ampm = "";
    if (hour == 0) {
      ampm = parent.getString(R.string.midnight);
      hour = 12;
    } else if (hour == 12) {
      ampm = parent.getString(R.string.noon);
    } else if (hour > 12) {
      hour = hour - 12;
      ampm = parent.getString(R.string.pm);
    } else {
      ampm = parent.getString(R.string.am);
    }
    try {
      boolean canSpeak = speakingTimeLock.tryLock(1000, TimeUnit.MILLISECONDS);
      if (canSpeak) {
        tts.speak(Integer.toString(hour), 0, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak(Integer.toString(minutes), 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak("[slnc]", 1, null);
        tts.speak(ampm, 1, null);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      speakingTimeLock.unlock();
    }
  }

  public void toggleAirplaneMode() {
    boolean setAirPlaneMode = !airplaneModeEnabled();
    if (!setAirPlaneMode) {
      tts.speak(parent.getString(R.string.disabled), 1, null);
    } else {
      tts.speak(parent.getString(R.string.enabled), 1, null);
    }
    Settings.System.putInt(parent.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
        setAirPlaneMode ? 1 : 0);
    Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    intent.putExtra("state", setAirPlaneMode);
    parent.sendBroadcast(intent);
  }

  public boolean airplaneModeEnabled() {
    ContentResolver cr = parent.getContentResolver();
    int x;
    try {
      x = Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON);
      if (x == 1) {
        return true;
      }
    } catch (SettingNotFoundException e) {
      // This setting is always there as it is part of the Android framework;
      // therefore, this exception is not reachable and nothing special needs
      // to be done here.
      e.printStackTrace();
    }
    return false;
  }

  public void announceWeather() {
    int version = 0;
    try {
      URLConnection cn;
      URL url = new URL("http://www.weather.gov/xml/current_obs/KPAO.rss");
      cn = url.openConnection();
      cn.connect();
      InputStream stream = cn.getInputStream();
      DocumentBuilder docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document weatherRssDoc = docBuild.parse(stream);
      NodeList titles = weatherRssDoc.getElementsByTagName("title");
      NodeList descriptions = weatherRssDoc.getElementsByTagName("description");
      String title = titles.item(2).getFirstChild().getNodeValue();
      String description = descriptions.item(1).getChildNodes().item(2).getNodeValue();
      tts.speak(title, 0, null);
      tts.speak(description, 1, null);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FactoryConfigurationError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void callVoiceMail() {
    // Uri phoneNumberURI = Uri.parse("tel:" +
    // Uri.encode(parent.voiceMailNumber));

    Uri phoneNumberURI = Uri.parse("tel:" + Uri.encode("18056377243"));
    Intent intent = new Intent(Intent.ACTION_CALL, phoneNumberURI);
    parent.startActivity(intent);
  }

}