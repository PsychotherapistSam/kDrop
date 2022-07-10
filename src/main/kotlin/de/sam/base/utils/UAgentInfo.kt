/* *******************************************
// Copyright 2010-2015, Anthony Hand
//
//
// File version 2015.05.13 (May 13, 2015)
// Updates:
//	- Moved MobileESP to GitHub. https://github.com/ahand/mobileesp
//	- Opera Mobile/Mini browser has the same UA string on multiple platforms and doesn't differentiate phone vs. tablet.
//		- Removed DetectOperaAndroidPhone(). This method is no longer reliable.
//		- Removed DetectOperaAndroidTablet(). This method is no longer reliable.
//	- Added support for Windows Phone 10: variable and DetectWindowsPhone10()
//	- Updated DetectWindowsPhone() to include WP10.
//	- Added support for Firefox OS.
//		- A variable plus DetectFirefoxOS(), DetectFirefoxOSPhone(), DetectFirefoxOSTablet()
//		- NOTE: Firefox doesn't add UA tokens to definitively identify Firefox OS vs. their browsers on other mobile platforms.
//	- Added support for Sailfish OS. Not enough info to add a tablet detection method at this time.
//		- A variable plus DetectSailfish(), DetectSailfishPhone()
//	- Added support for Ubuntu Mobile OS.
//		- DetectUbuntu(), DetectUbuntuPhone(), DetectUbuntuTablet()
//	- Added support for 2 smart TV OSes. They lack browsers but do have WebViews for use by HTML apps.
//		- One variable for Samsung Tizen TVs, plus DetectTizenTV()
//		- One variable for LG WebOS TVs, plus DetectWebOSTV()
//	- Updated DetectTizen(). Now tests for "mobile" to disambiguate from Samsung Smart TVs
//	- Removed variables for obsolete devices: deviceHtcFlyer, deviceXoom.
//	- Updated DetectAndroid(). No longer has a special test case for the HTC Flyer tablet.
//	- Updated DetectAndroidPhone().
//		- Updated internal detection code for Android.
//		- No longer has a special test case for the HTC Flyer tablet.
//		- Checks against DetectOperaMobile() on Android and reports here if relevant.
//	- Updated DetectAndroidTablet().
//		- No longer has a special test case for the HTC Flyer tablet.
//		- Checks against DetectOperaMobile() on Android to exclude it from here.
//	- DetectMeego(): Changed definition for this method. Now detects any Meego OS device, not just phones.
//	- DetectMeegoPhone(): NEW. For Meego phones. Ought to detect Opera browsers on Meego, as well.
//	- DetectTierIphone(): Added support for phones running Sailfish, Ubuntu and Firefox Mobile.
//	- DetectTierTablet(): Added support for tablets running Ubuntu and Firefox Mobile.
//	- DetectSmartphone(): Added support for Meego phones.
//	- Refactored the detection logic in DetectMobileQuick() and DetectMobileLong().
//		- Moved a few detection tests for older browsers to Long.
//
//
//
// LICENSE INFORMATION
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//        http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific
// language governing permissions and limitations under the License.
//
//
// ABOUT THIS PROJECT
//   Project Owner: Anthony Hand
//   Email: anthony.hand@gmail.com
//   Web Site: http://www.mobileesp.com
//   Source Files: https://github.com/ahand/mobileesp
//
//   Versions of this code are available for:
//      PHP, JavaScript, Java, ASP.NET (C#), and Ruby
//
// *******************************************
 */

package de.sam.base.utils

import java.util.*

/**
 * The DetectSmartPhone class encapsulates information about
 * a browser's connection to your web site.
 * You can use it to find out whether the browser asking for
 * your site's content is probably running on a mobile device.
 * The methods were written so you can be as granular as you want.
 * For example, enquiring whether it's as specific as an iPod Touch or
 * as general as a smartphone class device.
 * The object's methods return true, or false.
 */
class UAgentInfo(userAgent: String?, httpAccept: String?) {
    /**
     * Return the lower case HTTP_USER_AGENT
     * @return userAgent
     */
    // User-Agent and Accept HTTP request headers
    var userAgent = ""

    /**
     * Return the lower case HTTP_ACCEPT
     * @return httpAccept
     */
    var httpAccept = ""

    // Let's store values for quickly accessing the same info multiple times.
    var initCompleted = false
    var isWebkit = false //Stores the result of DetectWebkit()
    var isMobilePhone = false //Stores the result of DetectMobileQuick()

    /**
     * Return whether the device is an Iphone or iPod Touch
     * @return isIphone
     */
    var isIphone = false //Stores the result of DetectIphone()
    var isAndroid = false //Stores the result of DetectAndroid()
    var isAndroidPhone = false //Stores the result of DetectAndroidPhone()

    /**
     * Return whether the device is in the Tablet Tier.
     * @return isTierTablet
     */
    var isTierTablet = false //Stores the result of DetectTierTablet()

    /**
     * Return whether the device is in the Iphone Tier.
     * @return isTierIphone
     */
    var isTierIphone = false //Stores the result of DetectTierIphone()

    /**
     * Return whether the device is in the 'Rich CSS' tier of mobile devices.
     * @return isTierRichCss
     */
    var isTierRichCss = false //Stores the result of DetectTierRichCss()

    /**
     * Return whether the device is a generic, less-capable mobile device.
     * @return isTierGenericMobile
     */
    var isTierGenericMobile = false //Stores the result of DetectTierOtherPhones()

    /**
     * Initialize the userAgent and httpAccept variables
     *
     * @param userAgent the User-Agent header
     * @param httpAccept the Accept header
     */
    init {
        if (userAgent != null) {
            this.userAgent = userAgent.lowercase(Locale.getDefault())
        }
        if (httpAccept != null) {
            this.httpAccept = httpAccept.lowercase(Locale.getDefault())
        }

        //Intialize key stored values.
        initDeviceScan()
    }

    /**
     * Initialize Key Stored Values.
     */
    fun initDeviceScan() {
        //Save these properties to speed processing
        isWebkit = detectWebkit()
        isIphone = detectIphone()
        isAndroid = detectAndroid()
        isAndroidPhone = detectAndroidPhone()

        //Generally, these tiers are the most useful for web development
        isMobilePhone = detectMobileQuick()
        isTierTablet = detectTierTablet()
        isTierIphone = detectTierIphone()

        //Optional: Comment these out if you NEVER use them
        isTierRichCss = detectTierRichCss()
        isTierGenericMobile = detectTierOtherPhones()
        initCompleted = true
    }

    /**
     * Detects if the current device is an iPhone.
     * @return detection of an iPhone
     */
    fun detectIphone(): Boolean {
        // The iPad and iPod touch say they're an iPhone! So let's disambiguate.
        return userAgent.indexOf(deviceIphone) != -1 && !detectIpad() && !detectIpod()
    }

    /**
     * Detects if the current device is an iPod Touch.
     * @return detection of an iPod Touch
     */
    fun detectIpod(): Boolean {
        return userAgent.indexOf(deviceIpod) != -1
    }

    /**
     * Detects if the current device is an iPad tablet.
     * @return detection of an iPad
     */
    fun detectIpad(): Boolean {
        return (userAgent.indexOf(deviceIpad) != -1 && detectWebkit())
    }

    /**
     * Detects if the current device is an iPhone or iPod Touch.
     * @return detection of an iPhone or iPod Touch
     */
    fun detectIphoneOrIpod(): Boolean {
        //We repeat the searches here because some iPods may report themselves as an iPhone, which would be okay.
        return (userAgent.indexOf(deviceIphone) != -1 || userAgent.indexOf(deviceIpod) != -1)
    }

    /**
     * Detects *any* iOS device: iPhone, iPod Touch, iPad.
     * @return detection of an Apple iOS device
     */
    fun detectIos(): Boolean {
        return detectIphoneOrIpod() || detectIpad()
    }

    /**
     * Detects *any* Android OS-based device: phone, tablet, and multi-media player.
     * Also detects Google TV.
     * @return detection of an Android device
     */
    fun detectAndroid(): Boolean {
        return userAgent.indexOf(deviceAndroid) != -1 || detectGoogleTV()
    }

    /**
     * Detects if the current device is a (small-ish) Android OS-based device
     * used for calling and/or multi-media (like a Samsung Galaxy Player).
     * Google says these devices will have 'Android' AND 'mobile' in user agent.
     * Ignores tablets (Honeycomb and later).
     * @return  detection of an Android phone
     */
    fun detectAndroidPhone(): Boolean {
        //First, let's make sure we're on an Android device.
        if (!detectAndroid()) return false

        //If it's Android and has 'mobile' in it, Google says it's a phone.
        if (userAgent.indexOf(mobile) != -1) return true

        //Special check for Android devices with Opera Mobile/Mini. They should report here.
        return detectOperaMobile()
    }

    /**
     * Detects if the current device is a (self-reported) Android tablet.
     * Google says these devices will have 'Android' and NOT 'mobile' in their user agent.
     * @return detection of an Android tablet
     */
    fun detectAndroidTablet(): Boolean {
        //First, let's make sure we're on an Android device.
        if (!detectAndroid()) return false

        //Special check for Android devices with Opera Mobile/Mini. They should NOT report here.
        if (detectOperaMobile()) return false

        //Otherwise, if it's Android and does NOT have 'mobile' in it, Google says it's a tablet.
        return userAgent.indexOf(mobile) <= -1
    }

    /**
     * Detects if the current device is an Android OS-based device and
     * the browser is based on WebKit.
     * @return detection of an Android WebKit browser
     */
    fun detectAndroidWebKit(): Boolean {
        return detectAndroid() && detectWebkit()
    }

    /**
     * Detects if the current device is a GoogleTV.
     * @return detection of GoogleTV
     */
    fun detectGoogleTV(): Boolean {
        return userAgent.indexOf(deviceGoogleTV) != -1
    }

    /**
     * Detects if the current browser is based on WebKit.
     * @return detection of a WebKit browser
     */
    fun detectWebkit(): Boolean {
        return userAgent.indexOf(engineWebKit) != -1
    }

    /**
     * Detects if the current browser is the Symbian S60 Open Source Browser.
     * @return detection of Symbian S60 Browser
     */
    fun detectS60OssBrowser(): Boolean {
        //First, test for WebKit, then make sure it's either Symbian or S60.
        return (detectWebkit()
                && (userAgent.indexOf(deviceSymbian) != -1
                || userAgent.indexOf(deviceS60) != -1))
    }

    /**
     *
     * Detects if the current device is any Symbian OS-based device,
     * including older S60, Series 70, Series 80, Series 90, and UIQ,
     * or other browsers running on these devices.
     * @return detection of SymbianOS
     */
    fun detectSymbianOS(): Boolean {
        return userAgent.indexOf(deviceSymbian) != -1
                || userAgent.indexOf(deviceS60) != -1
                || userAgent.indexOf(deviceS70) != -1
                || userAgent.indexOf(deviceS80) != -1
                || userAgent.indexOf(deviceS90) != -1
    }

    /**
     * Detects if the current browser is a Windows Phone 7.x, 8, or 10 device
     * @return detection of Windows Phone 7.x OR 8
     */
    fun detectWindowsPhone(): Boolean {
        return (detectWindowsPhone7() || detectWindowsPhone8() || detectWindowsPhone10())
    }

    /**
     * Detects a Windows Phone 7 device (in mobile browsing mode).
     * @return detection of Windows Phone 7
     */
    fun detectWindowsPhone7(): Boolean {
        return userAgent.indexOf(deviceWinPhone7) != -1
    }

    /**
     * Detects a Windows Phone 8 device (in mobile browsing mode).
     * @return detection of Windows Phone 8
     */
    fun detectWindowsPhone8(): Boolean {
        return userAgent.indexOf(deviceWinPhone8) != -1
    }

    /**
     * Detects a Windows Phone 10 device (in mobile browsing mode).
     * @return detection of Windows Phone 10
     */
    fun detectWindowsPhone10(): Boolean {
        return userAgent.indexOf(deviceWinPhone10) != -1
    }

    /**
     * Detects if the current browser is a Windows Mobile device.
     * Excludes Windows Phone 7.x and 8 devices.
     * Focuses on Windows Mobile 6.xx and earlier.
     * @return detection of Windows Mobile
     */
    fun detectWindowsMobile(): Boolean {
        if (detectWindowsPhone()) {
            return false
        }
        //Most devices use 'Windows CE', but some report 'iemobile'
        //  and some older ones report as 'PIE' for Pocket IE.
        //  We also look for instances of HTC and Windows for many of their WinMo devices.
        if ((userAgent.indexOf(deviceWinMob) != -1
                    || userAgent.indexOf(deviceWinMob) != -1
                    || userAgent.indexOf(deviceIeMob) != -1
                    || userAgent.indexOf(enginePie) != -1
                    || userAgent.indexOf(manuHtc) != -1)
            && userAgent.indexOf(deviceWindows) != -1
            || detectWapWml()
            && userAgent.indexOf(deviceWindows) != -1
        ) {
            return true
        }

        //Test for Windows Mobile PPC but not old Macintosh PowerPC.
        return userAgent.indexOf(devicePpc) != -1 && userAgent.indexOf(deviceMacPpc) == -1
    }

    /**
     * Detects if the current browser is any BlackBerry.
     * Includes BB10 OS, but excludes the PlayBook.
     * @return detection of Blackberry
     */
    fun detectBlackBerry(): Boolean {
        if (userAgent.indexOf(deviceBB) != -1 || httpAccept.indexOf(vndRIM) != -1) return true
        return detectBlackBerry10Phone()
    }

    /**
     * Detects if the current browser is a BlackBerry 10 OS phone.
     * Excludes tablets.
     * @return detection of a Blackberry 10 device
     */
    fun detectBlackBerry10Phone(): Boolean {
        return userAgent.indexOf(deviceBB10) != -1 && userAgent.indexOf(mobile) != -1
    }

    /**
     * Detects if the current browser is on a BlackBerry tablet device.
     * Example: PlayBook
     * @return detection of a Blackberry Tablet
     */
    fun detectBlackBerryTablet(): Boolean {
        return userAgent.indexOf(deviceBBPlaybook) != -1
    }

    /**
     * Detects if the current browser is a BlackBerry device AND uses a
     * WebKit-based browser. These are signatures for the new BlackBerry OS 6.
     * Examples: Torch. Includes the Playbook.
     * @return detection of a Blackberry device with WebKit browser
     */
    fun detectBlackBerryWebKit(): Boolean {
        return detectBlackBerry() && userAgent.indexOf(engineWebKit) != -1
    }

    /**
     * Detects if the current browser is a BlackBerry Touch
     * device, such as the Storm, Torch, and Bold Touch. Excludes the Playbook.
     * @return detection of a Blackberry touchscreen device
     */
    fun detectBlackBerryTouch(): Boolean {
        return detectBlackBerry() &&
                (userAgent.indexOf(deviceBBStorm) != -1
                        || userAgent.indexOf(deviceBBTorch) != -1
                        || userAgent.indexOf(deviceBBBoldTouch) != -1
                        || userAgent.indexOf(deviceBBCurveTouch) != -1)
    }

    /**
     * Detects if the current browser is a BlackBerry device AND
     * has a more capable recent browser. Excludes the Playbook.
     * Examples, Storm, Bold, Tour, Curve2
     * Excludes the new BlackBerry OS 6 and 7 browser!!
     * @return detection of a Blackberry device with a better browser
     */
    fun detectBlackBerryHigh(): Boolean {
        //Disambiguate for BlackBerry OS 6 or 7 (WebKit) browser
        if (detectBlackBerryWebKit()) return false
        return if (detectBlackBerry()) {
            detectBlackBerryTouch() || userAgent.indexOf(deviceBBBold) != -1 || userAgent.indexOf(deviceBBTour) != -1 ||
                    userAgent.indexOf(deviceBBCurve) != -1
        } else {
            false
        }
    }

    /**
     * Detects if the current browser is a BlackBerry device AND
     * has an older, less capable browser.
     * Examples: Pearl, 8800, Curve1
     * @return detection of a Blackberry device with a poorer browser
     */
    fun detectBlackBerryLow(): Boolean {
        return if (detectBlackBerry()) {
            //Assume that if it's not in the High tier, then it's Low
            !(detectBlackBerryHigh() || detectBlackBerryWebKit())
        } else {
            false
        }
    }

    /**
     * Detects if the current browser is on a PalmOS device.
     * @return detection of a PalmOS device
     */
    fun detectPalmOS(): Boolean {
        //Most devices nowadays report as 'Palm', but some older ones reported as Blazer or Xiino.
        return if (userAgent.indexOf(devicePalm) != -1 || userAgent.indexOf(engineBlazer) != -1 || userAgent.indexOf(
                engineXiino
            ) != -1
        ) {
            //Make sure it's not WebOS first
            !detectPalmWebOS()
        } else false
    }

    /**
     * Detects if the current browser is on a Palm device
     * running the new WebOS.
     * @return detection of a Palm WebOS device
     */
    fun detectPalmWebOS(): Boolean {
        return userAgent.indexOf(deviceWebOS) != -1
    }

    /**
     * Detects if the current browser is on an HP tablet running WebOS.
     * @return detection of an HP WebOS tablet
     */
    fun detectWebOSTablet(): Boolean {
        return userAgent.indexOf(deviceWebOShp) != -1 &&
                userAgent.indexOf(deviceTablet) != -1
    }

    /**
     * Detects if the current browser is on a WebOS smart TV.
     * @return detection of a WebOS smart TV
     */
    fun detectWebOSTV(): Boolean {
        return userAgent.indexOf(deviceWebOStv) != -1 && userAgent.indexOf(smartTV2) != -1
    }

    /**
     * Detects Opera Mobile or Opera Mini.
     * @return detection of an Opera browser for a mobile device
     */
    fun detectOperaMobile(): Boolean {
        return (userAgent.indexOf(engineOpera) != -1
                && (userAgent.indexOf(mini) != -1
                || userAgent.indexOf(mobi) != -1))
    }

    /**
     * Detects if the current device is an Amazon Kindle (eInk devices only).
     * Note: For the Kindle Fire, use the normal Android methods.
     * @return detection of a Kindle
     */
    fun detectKindle(): Boolean {
        return userAgent.indexOf(deviceKindle) != -1 && !detectAndroid()
    }

    /**
     * Detects if the current Amazon device is using the Silk Browser.
     * Note: Typically used by the the Kindle Fire.
     * @return detection of an Amazon Kindle Fire in Silk mode.
     */
    fun detectAmazonSilk(): Boolean {
        return userAgent.indexOf(engineSilk) != -1
    }

    /**
     * Detects if the current browser is a
     * Garmin Nuvifone.
     * @return detection of a Garmin Nuvifone
     */
    fun detectGarminNuvifone(): Boolean {
        return userAgent.indexOf(deviceNuvifone) != -1
    }

    /**
     * Detects a device running the Bada OS from Samsung.
     * @return detection of a Bada device
     */
    fun detectBada(): Boolean {
        return userAgent.indexOf(deviceBada) != -1
    }

    /**
     * Detects a device running the Tizen smartphone OS.
     * @return detection of a Tizen device
     */
    fun detectTizen(): Boolean {
        return userAgent.indexOf(deviceTizen) != -1 && userAgent.indexOf(mobile) != -1
    }

    /**
     * Detects if the current browser is on a Tizen smart TV.
     * @return detection of a Tizen smart TV
     */
    fun detectTizenTV(): Boolean {
        return userAgent.indexOf(deviceTizen) != -1 && userAgent.indexOf(smartTV1) != -1
    }

    /**
     * Detects a device running the Meego OS.
     * @return detection of a Meego device
     */
    fun detectMeego(): Boolean {
        return userAgent.indexOf(deviceMeego) != -1
    }

    /**
     * Detects a phone running the Meego OS.
     * @return detection of a Meego phone
     */
    fun detectMeegoPhone(): Boolean {
        return userAgent.indexOf(deviceMeego) != -1 && userAgent.indexOf(mobi) != -1
    }

    /**
     * Detects a mobile device (probably) running the Firefox OS.
     * @return detection of a Firefox OS mobile device
     */
    fun detectFirefoxOS(): Boolean {
        return detectFirefoxOSPhone() || detectFirefoxOSTablet()
    }

    /**
     * Detects a phone (probably) running the Firefox OS.
     * @return detection of a Firefox OS phone
     */
    fun detectFirefoxOSPhone(): Boolean {
        //First, let's make sure we're NOT on another major mobile OS.
        if (detectIos()
            || detectAndroid()
            || detectSailfish()
        ) return false
        return userAgent.indexOf(engineFirefox) != -1 && userAgent.indexOf(mobile) != -1
    }

    /**
     * Detects a tablet (probably) running the Firefox OS.
     * @return detection of a Firefox OS tablet
     */
    fun detectFirefoxOSTablet(): Boolean {
        //First, let's make sure we're NOT on another major mobile OS.
        if (detectIos()
            || detectAndroid()
            || detectSailfish()
        ) return false
        return userAgent.indexOf(engineFirefox) != -1 && userAgent.indexOf(deviceTablet) != -1
    }

    /**
     * Detects a device running the Sailfish OS.
     * @return detection of a Sailfish device
     */
    fun detectSailfish(): Boolean {
        return userAgent.indexOf(deviceSailfish) != -1
    }

    /**
     * Detects a phone running the Sailfish OS.
     * @return detection of a Sailfish phone
     */
    fun detectSailfishPhone(): Boolean {
        return detectSailfish() && userAgent.indexOf(mobile) != -1
    }

    /**
     * Detects a mobile device running the Ubuntu Mobile OS.
     * @return detection of an Ubuntu Mobile OS mobile device
     */
    fun detectUbuntu(): Boolean {
        return detectUbuntuPhone() || detectUbuntuTablet()
    }

    /**
     * Detects a phone running the Ubuntu Mobile OS.
     * @return detection of an Ubuntu Mobile OS phone
     */
    fun detectUbuntuPhone(): Boolean {
        return userAgent.indexOf(deviceUbuntu) != -1 && userAgent.indexOf(mobile) != -1
    }

    /**
     * Detects a tablet running the Ubuntu Mobile OS.
     * @return detection of an Ubuntu Mobile OS tablet
     */
    fun detectUbuntuTablet(): Boolean {
        return userAgent.indexOf(deviceUbuntu) != -1 && userAgent.indexOf(deviceTablet) != -1
    }

    /**
     * Detects the Danger Hiptop device.
     * @return detection of a Danger Hiptop
     */
    fun detectDangerHiptop(): Boolean {
        return (userAgent.indexOf(deviceDanger) != -1 || userAgent.indexOf(deviceHiptop) != -1)
    }

    /**
     * Detects if the current browser is a Sony Mylo device.
     * @return detection of a Sony Mylo device
     */
    fun detectSonyMylo(): Boolean {
        return (userAgent.indexOf(manuSony) != -1
                && (userAgent.indexOf(qtembedded) != -1
                || userAgent.indexOf(mylocom2) != -1))
    }

    /**
     * Detects if the current device is on one of the Maemo-based Nokia Internet Tablets.
     * @return detection of a Maemo OS tablet
     */
    fun detectMaemoTablet(): Boolean {
        if (userAgent.indexOf(maemo) != -1) {
            return true
        } else if (userAgent.indexOf(linux) != -1 && userAgent.indexOf(deviceTablet) != -1 && !detectWebOSTablet()
            && !detectAndroid()
        ) {
            return true
        }
        return false
    }

    /**
     * Detects if the current device is an Archos media player/Internet tablet.
     * @return detection of an Archos media player
     */
    fun detectArchos(): Boolean {
        return userAgent.indexOf(deviceArchos) != -1
    }

    /**
     * Detects if the current device is an Internet-capable game console.
     * Includes many handheld consoles.
     * @return detection of any Game Console
     */
    fun detectGameConsole(): Boolean {
        return (detectSonyPlaystation()
                || detectNintendo()
                || detectXbox())
    }

    /**
     * Detects if the current device is a Sony Playstation.
     * @return detection of Sony Playstation
     */
    fun detectSonyPlaystation(): Boolean {
        return userAgent.indexOf(devicePlaystation) != -1
    }

    /**
     * Detects if the current device is a handheld gaming device with
     * a touchscreen and modern iPhone-class browser. Includes the Playstation Vita.
     * @return detection of a handheld gaming device
     */
    fun detectGamingHandheld(): Boolean {
        return userAgent.indexOf(devicePlaystation) != -1 && userAgent.indexOf(devicePlaystationVita) != -1
    }

    /**
     * Detects if the current device is a Nintendo game device.
     * @return detection of Nintendo
     */
    fun detectNintendo(): Boolean {
        return userAgent.indexOf(deviceNintendo) != -1
                || userAgent.indexOf(deviceWii) != -1
                || userAgent.indexOf(deviceNintendoDs) != -1
    }

    /**
     * Detects if the current device is a Microsoft Xbox.
     * @return detection of Xbox
     */
    fun detectXbox(): Boolean {
        return userAgent.indexOf(deviceXbox) != -1
    }

    /**
     * Detects whether the device is a Brew-powered device.
     * @return detection of a Brew device
     */
    fun detectBrewDevice(): Boolean {
        return userAgent.indexOf(deviceBrew) != -1
    }

    /**
     * Detects whether the device supports WAP or WML.
     * @return detection of a WAP- or WML-capable device
     */
    fun detectWapWml(): Boolean {
        return (httpAccept.indexOf(vndwap) != -1
                || httpAccept.indexOf(wml) != -1)
    }

    /**
     * Detects if the current device supports MIDP, a mobile Java technology.
     * @return detection of a MIDP mobile Java-capable device
     */
    fun detectMidpCapable(): Boolean {
        return (userAgent.indexOf(deviceMidp) != -1
                || httpAccept.indexOf(deviceMidp) != -1)
    }
    //*****************************
    // Device Classes
    //*****************************
    /**
     * Check to see whether the device is any device
     * in the 'smartphone' category.
     * @return detection of a general smartphone device
     */
    fun detectSmartphone(): Boolean {
        //Exclude duplicates from TierIphone
        return (detectTierIphone()
                || detectS60OssBrowser()
                || detectSymbianOS()
                || detectWindowsMobile()
                || detectBlackBerry()
                || detectMeegoPhone()
                || detectPalmOS())
    }

    /**
     * Detects if the current device is a mobile device.
     * This method catches most of the popular modern devices.
     * Excludes Apple iPads and other modern tablets.
     * @return detection of any mobile device using the quicker method
     */
    fun detectMobileQuick(): Boolean {
        //Let's exclude tablets
        if (isTierTablet) {
            return false
        }
        //Most mobile browsing is done on smartphones
        if (detectSmartphone()) {
            return true
        }

        //Catch-all for many mobile devices
        if (userAgent.indexOf(mobile) != -1) {
            return true
        }
        if (detectOperaMobile()) {
            return true
        }

        //We also look for Kindle devices
        if (detectKindle()
            || detectAmazonSilk()
        ) {
            return true
        }
        if (detectWapWml()
            || detectMidpCapable()
            || detectBrewDevice()
        ) {
            return true
        }
        return userAgent.indexOf(engineNetfront) != -1 || userAgent.indexOf(engineUpBrowser) != -1
    }

    /**
     * The longer and more thorough way to detect for a mobile device.
     * Will probably detect most feature phones,
     * smartphone-class devices, Internet Tablets,
     * Internet-enabled game consoles, etc.
     * This ought to catch a lot of the more obscure and older devices, also --
     * but no promises on thoroughness!
     * @return detection of any mobile device using the more thorough method
     */
    fun detectMobileLong(): Boolean {
        if (detectMobileQuick()
            || detectGameConsole()
        ) {
            return true
        }
        if (detectDangerHiptop()
            || detectMaemoTablet()
            || detectSonyMylo()
            || detectArchos()
        ) {
            return true
        }
        if (userAgent.indexOf(devicePda) != -1 && userAgent.indexOf(disUpdate) < 0) //no index found
        {
            return true
        }

        //Detect older phones from certain manufacturers and operators.
        return userAgent.indexOf(uplink) != -1
                || userAgent.indexOf(engineOpenWeb) != -1
                || userAgent.indexOf(manuSamsung1) != -1
                || userAgent.indexOf(manuSonyEricsson) != -1
                || userAgent.indexOf(manuericsson) != -1
                || userAgent.indexOf(svcDocomo) != -1
                || userAgent.indexOf(svcKddi) != -1
                || userAgent.indexOf(svcVodafone) != -1
    }
    //*****************************
    // For Mobile Web Site Design
    //*****************************
    /**
     * The quick way to detect for a tier of devices.
     * This method detects for the new generation of
     * HTML 5 capable, larger screen tablets.
     * Includes iPad, Android (e.g., Xoom), BB Playbook, WebOS, etc.
     * @return detection of any device in the Tablet Tier
     */
    fun detectTierTablet(): Boolean {
        return (detectIpad()
                || detectAndroidTablet()
                || detectBlackBerryTablet()
                || detectFirefoxOSTablet()
                || detectUbuntuTablet()
                || detectWebOSTablet())
    }

    /**
     * The quick way to detect for a tier of devices.
     * This method detects for devices which can
     * display iPhone-optimized web content.
     * Includes iPhone, iPod Touch, Android, Windows Phone 7 and 8, BB10, WebOS, Playstation Vita, etc.
     * @return detection of any device in the iPhone/Android/Windows Phone/BlackBerry/WebOS Tier
     */
    fun detectTierIphone(): Boolean {
        return (detectIphoneOrIpod()
                || detectAndroidPhone()
                || detectWindowsPhone()
                || detectBlackBerry10Phone()
                || (detectBlackBerryWebKit()
                && detectBlackBerryTouch())
                || detectPalmWebOS()
                || detectBada()
                || detectTizen()
                || detectFirefoxOSPhone()
                || detectSailfishPhone()
                || detectUbuntuPhone()
                || detectGamingHandheld())
    }

    /**
     * The quick way to detect for a tier of devices.
     * This method detects for devices which are likely to be capable
     * of viewing CSS content optimized for the iPhone,
     * but may not necessarily support JavaScript.
     * Excludes all iPhone Tier devices.
     * @return detection of any device in the 'Rich CSS' Tier
     */
    fun detectTierRichCss(): Boolean {
        var result = false
        //The following devices are explicitly ok.
        //Note: 'High' BlackBerry devices ONLY
        if (detectMobileQuick()) {

            //Exclude iPhone Tier and e-Ink Kindle devices.
            if (!detectTierIphone() && !detectKindle()) {

                //The following devices are explicitly ok.
                //Note: 'High' BlackBerry devices ONLY
                //Older Windows 'Mobile' isn't good enough for iPhone Tier.
                if ((detectWebkit()
                            || detectS60OssBrowser()
                            || detectBlackBerryHigh()
                            || detectWindowsMobile()) || userAgent.indexOf(engineTelecaQ) != -1
                ) {
                    result = true
                } // if detectWebkit()
            } //if !detectTierIphone()
        } //if detectMobileQuick()
        return result
    }

    /**
     * The quick way to detect for a tier of devices.
     * This method detects for all other types of phones,
     * but excludes the iPhone and RichCSS Tier devices.
     * @return detection of a mobile device in the less capable tier
     */
    fun detectTierOtherPhones(): Boolean {
        //Exclude devices in the other 2 categories
        return (detectMobileLong()
                && !detectTierIphone()
                && !detectTierRichCss())
    }

    companion object {
        // Initialize some initial smartphone string variables.
        const val engineWebKit = "webkit"
        const val deviceIphone = "iphone"
        const val deviceIpod = "ipod"
        const val deviceIpad = "ipad"
        const val deviceMacPpc = "macintosh" //Used for disambiguation
        const val deviceAndroid = "android"
        const val deviceGoogleTV = "googletv"
        const val deviceWinPhone7 = "windows phone os 7"
        const val deviceWinPhone8 = "windows phone 8"
        const val deviceWinPhone10 = "windows phone 10"
        const val deviceWinMob = "windows ce"
        const val deviceWindows = "windows"
        const val deviceIeMob = "iemobile"
        const val devicePpc = "ppc" //Stands for PocketPC
        const val enginePie = "wm5 pie" //An old Windows Mobile
        const val deviceBB = "blackberry"
        const val deviceBB10 = "bb10" //For the new BB 10 OS
        const val vndRIM = "vnd.rim" //Detectable when BB devices emulate IE or Firefox
        const val deviceBBStorm = "blackberry95" //Storm 1 and 2
        const val deviceBBBold = "blackberry97" //Bold 97x0 (non-touch)
        const val deviceBBBoldTouch = "blackberry 99" //Bold 99x0 (touchscreen)
        const val deviceBBTour = "blackberry96" //Tour
        const val deviceBBCurve = "blackberry89" //Curve 2
        const val deviceBBCurveTouch = "blackberry 938" //Curve Touch 9380
        const val deviceBBTorch = "blackberry 98" //Torch
        const val deviceBBPlaybook = "playbook" //PlayBook tablet
        const val deviceSymbian = "symbian"
        const val deviceS60 = "series60"
        const val deviceS70 = "series70"
        const val deviceS80 = "series80"
        const val deviceS90 = "series90"
        const val devicePalm = "palm"
        const val deviceWebOS = "webos" //For Palm devices
        const val deviceWebOStv = "web0s" //For LG TVs
        const val deviceWebOShp = "hpwos" //For HP's line of WebOS devices
        const val deviceNuvifone = "nuvifone" //Garmin Nuvifone
        const val deviceBada = "bada" //Samsung's Bada OS
        const val deviceTizen = "tizen" //Tizen OS
        const val deviceMeego = "meego" //Meego OS
        const val deviceSailfish = "sailfish" //Sailfish OS
        const val deviceUbuntu = "ubuntu" //Ubuntu Mobile OS
        const val deviceKindle = "kindle" //Amazon Kindle, eInk one
        const val engineSilk = "silk-accelerated" //Amazon's accelerated Silk browser for Kindle Fire
        const val engineBlazer = "blazer" //Old Palm
        const val engineXiino = "xiino" //Another old Palm

        //Initialize variables for mobile-specific content.
        const val vndwap = "vnd.wap"
        const val wml = "wml"

        //Initialize variables for other random devices and mobile browsers.
        const val deviceTablet = "tablet" //Generic term for slate and tablet devices
        const val deviceBrew = "brew"
        const val deviceDanger = "danger"
        const val deviceHiptop = "hiptop"
        const val devicePlaystation = "playstation"
        const val devicePlaystationVita = "vita"
        const val deviceNintendoDs = "nitro"
        const val deviceNintendo = "nintendo"
        const val deviceWii = "wii"
        const val deviceXbox = "xbox"
        const val deviceArchos = "archos"
        const val engineFirefox = "firefox" //For Firefox OS
        const val engineOpera = "opera" //Popular browser
        const val engineNetfront = "netfront" //Common embedded OS browser
        const val engineUpBrowser = "up.browser" //common on some phones
        const val engineOpenWeb = "openweb" //Transcoding by OpenWave server
        const val deviceMidp = "midp" //a mobile Java technology
        const val uplink = "up.link"
        const val engineTelecaQ = "teleca q" //a modern feature phone browser
        const val devicePda = "pda" //some devices report themselves as PDAs
        const val mini = "mini" //Some mobile browsers put "mini" in their names.
        const val mobile = "mobile" //Some mobile browsers put "mobile" in their user agent strings.
        const val mobi = "mobi" //Some mobile browsers put "mobi" in their user agent strings.

        //Smart TV strings
        const val smartTV1 = "smart-tv" //Samsung Tizen smart TVs
        const val smartTV2 = "smarttv" //LG WebOS smart TVs

        //Use Maemo, Tablet, and Linux to test for Nokia"s Internet Tablets.
        const val maemo = "maemo"
        const val linux = "linux"
        const val qtembedded = "qt embedded" //for Sony Mylo
        const val mylocom2 = "com2" //for Sony Mylo also

        //In some UserAgents, the only clue is the manufacturer.
        const val manuSonyEricsson = "sonyericsson"
        const val manuericsson = "ericsson"
        const val manuSamsung1 = "sec-sgh"
        const val manuSony = "sony"
        const val manuHtc = "htc" //Popular Android and WinMo manufacturer

        //In some UserAgents, the only clue is the operator.
        const val svcDocomo = "docomo"
        const val svcKddi = "kddi"
        const val svcVodafone = "vodafone"

        //Disambiguation strings.
        const val disUpdate = "update" //pda vs. update
    }
}