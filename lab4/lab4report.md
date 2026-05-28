# Lab 3

## Part 1

> Briefly describe the permission system in Android. For example:
> How are permissions defined? provide an example and explain it.

Permissions are declared in AndroidManifest.xml using the `<uses-permission>` tag.
Example: `<uses-permission android:name="android.permission.INTERNET"/> <uses-permission android:name="android.permission.SEND_SMS"/>`
Basically telling Android this app needs to make network requests. Without this the system will block the apps attempt to send sms or access the internet

> What do they protect?

Sensitive data (contacts, SMS, location, photos) & system features (camera, microphone, internet access).

> Against who and what do they protect?

Against malicious apps that try to steal personal information, send premium SMS or access hardware without user consent. By isolating resources this permission system ensures apps can not perform harmful or privacy invasive actions without being granted extra permission by user agreement.

> What is the root of the problem? Why can you access this information?

Problem is that the Android permissions are not enough for protecting privacy, there are always workarounds through side channels. Android assumes that "public resources" (files the Linux kernel exposes for system coordination) are harmless and never needed a permission. /proc/net/arp is one of these — Linux treats ARP info as "just internal network state, not sensitive." But when combined with BSSID databases (Google/Skyhook/Navizon) the ARP entries become location data. Something that should be protected by ACCESS_FINE_LOCATION. While "geographic location" is protected by a permission, Wi-Fi hotspot properties (SSID and BSSID) are often accessible to any app without permissions. 

> Explain your implementation: In a few own words, what does your code do?

In addItem:
1. Open /proc/net/arp as a file, as its readable by all without permission
2. Skip the header (first line)
3. Parse each remaining line, split on whitespace to extract IP adress, MAC adress (BSSID) and device (wlan0)
4. Creates an Item for each entry showing bssid, MAC, and interface values.
5. Adds all items to the list and update UI (previous code)

In onYes
1. Collect all items from the list, make it a single comma seperated string
2. Encode it as UTF-8 just in case to handle special characters 
3. Append the string to the url as a query parameter
4. Create an ACTION_VIEW intent with this url 
5. Call startActivity() which tells Android to route it to the browser
6. The browser that has internet access makes the HTTP request as instructed, meaning our app never touches the network but we can still control what does. 


Some specifics we researched and looked up to follow through:

Section 2.2 Networking in the paper on Leviathans blog. Describes how to smuggle data out through internet with zero-permission app.
Section 4 of the paper, reading /proc/net/arp and /proc/net/wireless contains BSSID (the MAC address of the Wi-Fi access point you're connected to)

HW is MAC adress, aka the BSSID. This is the value the paper says you can feed to Skyhook/Navizon/Google's BSSID database to physically locate the device. So on a real device MAC would be the physical router connected on wifi.
radio0 is something from the Android emulator

    generic_x86_64:/ $ cat /proc/net/arp
    IP address       HW type     Flags       HW address            Mask     Device
    192.168.232.1    0x1         0x2         02:00:00:00:01:00     *        wlan0
    192.168.200.1    0x1         0x2         1a:cb:0f:ab:95:ce     *        radio0

> Which data do you retrieve? Where does it come from? What information does it contain? Why can you read it?

Data retrieved is BSSID (MAC adress), IP adress from /arp + the network interface 
The source of this data is from the resources shared across users/apps, provided by the Linux kernel, through /proc/net/arp.
Information contained is unique identifiers for networking hardware. That is the MAC adressed for network gateways. These MACs are sensitive to location.
According to the paper (Zhou et al.), the reason we can read it is to "facilitate coordination among apps and simplify access control". Since Android is built on top of Linux, and didn't add any permission check on top of this.

> How do you exfiltrate the data from the device? Why is no permission like INTERNET needed to send the data out?

We do it by building a URL with the data as a query parameter and then send it through the ACTION_VIEW intent to the browser. So we pass along the internet permission to another device that has it.  Because the app delegates the task of accessing internet to another application. In this case the Android web browser which is trusted by the system and has INTERNET permission, it loads the URL (which contains the sensitive data)

> Extra: Is it still exploitable on devices that run Android 10 or higher? If no, please explain the reason. If yes, would the attack scenario be the same?

Nope it does not work since Android 10, since they changed so the app would need ACCES_FINE_LOCATION permission during installation which would then alert the user of this. (With human error I suppose it still would work). 

## Part 2

> Briefly describe the code in DatabaseActivity.onCreate: 
> In your own words, what does the code do?
> How can an Intent influence method execution?
> What is the returned result value?

The onCreate function receives an request, checks that the source of the request is one that we allow through class name and Intent type. Then it checks the extra parameter (ITEM_ACTION) of the intent to select which action to take in the database. After an action, it sets the result to the result Intent. In the case of GET_ITEMS_ACTION, extra data will be put in the result intent, containing an ArrayList of Items, where each item comes from the Database.

> On an abstract level, what is the root of the problem? Why can you access the data?

The maclocation app does not really authenticate the requests in a secure way, making it relatively easy for other apps to request and get its internal data. The activity is exported, making it possible for other apps to call it. 

> Evaluate the existing checks in the code:
> What are the conditions in the if-statement in DatabaseActivity.onCreate checking for?

The if-statement checks three things and all must be true otherwise the request is rejected.

1. sourceIntent.getType() != null: The intent must have a type.
2. sourceIntent.getType().equals(TYPE): The intent's type must match the value lbs.lab.maclocation.DatabaseActivity
3. !getCallingActivity().getShortClassName().equals(getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent().getShortClassName()): compares Activity that launched this one and the apps launcher Activity. Note is that it is the Activitys short class name that must equal the launchers activitys short class name. So basiclly "Is the callers short name "MainActivity".

> Why are they not sufficient? How can we circumvent them?

They are not sufficient since the caller can set the type of the intent to any string, and use the same classname as maclocation (MainActivity) for the calling activity. 

> Explain your implementation of MainActivity.act: 
> Describe the Intent to call DatabaseActivity. Which are the important parameters to trick DatabaseActivity to expose the data?

We create an intent, with type set to "lbs.lab.maclocation.DatabaseActivity", with an extra parameter "ITEM_ACTION" with the value "GET_ITEMS_ACTION". We use setComponent to specify the activity we want to start ("lbs.lab.maclocation.DatabaseActivity"). The type needs to be correct because of the check in DatabaseActivity. The extra parameter specifies the action for the DatabaseActivity to take, which needs to be correct.


> Which role does the class name MainActivity play in the attack?

The attack app (MACIntent) also has a MainActivity so when it calls DatabaseActivity Android just sees that the caller is MainActivity (the attacker). Then DatabaseActivity checks if the short name of the class is "MainActivity", yes it is, check passes and it proceeds to give the unprivileged MainActivity allowance to execute and collect the data. 

> Briefly explain your implementation of MainActivity.onActivityResult.

onActivityResult simply reads the results from the intent response, parses it out the same way as MacLocation did, then creates an intent to do a GET request to an url in a web view, which contains the data from the database.

## Part 3

> Which system modifications can you implement on the Linux level?

**Making some public resources not accessible by no-permission apps**: A concrete solution to the attack in part 1 would be to require some permissions to access public resources provided by the Linux kernel. This would have forced us to declare this permission to access the ARP data for the attack in part 1. It is only a mitigation in the sense that most users probably would have allowed the permission if they wanted to run the app. Also, there exists many public resources, some that are different across devices and leaked by third party drivers. By implementing this, it would probably increase the complexity of the permission system, and break older apps. 

**IPC**: When any app or process recives data or task or intent to perform anything from a less privilege app or source, the Linux kernel can restrict the reciving apps permissions. Example of this would be when MACIntent sends data to DatabaseActivity the kernel sets the permissions of DatabaseActivity to the same level of MACIntent, and once the task is complete it reverts it back. This would completly kill any chances of using a deputy structure to perform such an "attack". Works across all applications without specific developer interaction or code. Complex to implement and could cause performance drops. Is also becomes annoying for the user and more permissions need to be managed more carefully for applications to work. This prevents any permission re-delegation attacks or deputy style. However its still possible to leak information that has been collected already or elsewhere. This is a protection.

**UID**: The UID of the sender of an intent should be embeded in the request by the kernel, meaning the receiver can reliably check that the UID is allowed to run it. This would strengthen the security boundary between apps so they can not access each others memory or files or description unless the Linux kernel sends an authorized ID. Con is that a developer need to take advantage of this system for it to have any effect, it may ruin apps that need to share UID. Pro is that an application can never lie about its UID to the Linux kernel. If the DatabaseActivity would use such an check, the attack of part 2 could not have happened. This is a protection

> Which system modifications can you implement on the Android API level?

**Permissions**: Change the BSSID from its current permission level (probably normal) to dangerous which would require user consent. This would remove the side-channel completly that is for Part 1. Can break legitimate apps that rely on something like wifi. This is a protection versus side-channel attacks (exploiting unprotected resources). Still possible to do like in part 2 with a confused deputy.

**IPC Inspection**: Make some changes to ActivityManager and PackageManager, so similar to earlier it reduces permissions down to that of the sender or rather the intersection between the two communicating services. CurrentPermissions(Deputy) = OriginalPermissions(Deputy) ∩ OriginalPermissions(Requester). Pro is that its always working even if devs dont "apply" it, handles less obvious attacks and also solves the entire Deputy situation problem. Cons is that its complex to implement correctly, permission bloating could be a thing in large scale because requesters need to ask for permission they will never use as part of their core purpose. Dosent fix return values. This is a protection and does so against permission re-delegation by enforcing the "intersection" lowering. Still possible to share data obtained or that is in shared storage. Does not stop one app with permission to "help" another malicious app. 

**Application instances**:  When an external Intent arrivies, automatically create a new instance of that app to handle it, this would be it runs completly seperates processes with restricted permissions. The initial primary instance stays completly unaffected. Prevents privilege request constantly popping up, allows the primary app to continue. Con is that its complex and requires multiple instances to run which affects performance (mostly memory probably), state management can become messy. This is a mitigation, dosent fix the security issues just makes it easier for the user. Protects against attacks that would cause the main application to fail. Can still cause permission loss in the new application instances. 

**Blame map**: A map that keeps a recording on which requester caused any permission reduction. This is more of a way to help the user, because if a request fails the map can prompt the user to give access to the initial service / app instead of the deputy. Risky if the user dosen't understand what they are giving permission to. Pro is that it gives the user alot more control and visibility. Can authenticate apps that mean no harm. Con is that it is annoying for the user and they might not understand the permission request. This is a mitigation by shifting the responsibility from defending vs the attack to the users control. Protects against invisible permission re-delegation. Still possible to trick the user.

> Which of these modifications fix Part 1? Which Part 2?

**PART1**: IPC Inspection because it reduces the database read permissions to that of the attackers (browser can exfiltrate data). Changing the permission to dangerous because it removes the side channel, part 1 requires location permission.

**PART2**: IPC inspection prevents the databse read completly. Application instances isolates privileged operations. Blame maps depends on the user to then deny or allow permissions re-delegation. UID prevents any cross application memory access. 

> Which configuration options exist for Android components to control their availability? Explain the relevant one(s) for Part 2.

In the AndroidManifest of MACLocation, the DatabaseActivity could be set to unexported. This would prevent other apps from calling it with Intents.
<activity android:name=".DatabaseActivity" android:exported="false" />

This makes sure that only our own app (or the system) can start this specific activity. This is really easy because its just one line to fix it, no performance cost. However it does completly kill any communication between app. So for example if a trusted app would like to legitimately check the databse it would be blocked / fail. This is a protection through and through by making it inaccessible. Protects from any external apps trying to launch it (such as MACIntent). It does not protect against any in-house buggs or security breaches (in the DatabaseActivity), so MainActivity. Or if this services places any information incorrectly in a shared resource. 

> How can you secure the code programmatically? For example, how can the conditions in the if-statement in DatabaseActivity.onCreate be corrected?

    if(!getPackageName().equals(getCallingPackage())) {
        // Not allowed
    } else {
        // continue
    }

This completly protects the app from any "spoofing" because in Android all apps have unique package names and looking in to the FULL name is important. Could 

> Are there other protections/mitigations?

You can create a custom permission for the intent in MACLocation, so that it requires apps to be signed with a certificate to use it. (In AndroidManifest: android:permission and android:protectionLevel="signature"). This way you could cryptographically only allow apps that you control to call the DatabaseActivity.

# Challanges

## Realsec

### Challenge 5

The site splits the filename on '.', and only checks the first word after the first '.'.
Hence we can choose a filename with many extensions, such as script.png.php containing the
payload, in which case the code will confirm that png is valid and allow the upload.

### Challenge 6

By running the following code in the terminal, then uploading a file containing the payload
(for example file from challenge 5), we will successfully upload the code.

    document.forms[5].onformdata = function(e){
        e.formData.set("file", new Blob([e.formData.get("file")], {type: "image/png"}), "file.php");
    }

The code works by intercepting the submit process, and setting the MIME type of the file to png.
When the server checks file type, it bases it only on the MIME type sent from the client.

### Challenge 8

By running:

    document.cookie="Tapeshlog=true"

we will be able to gain access, since the code only checks only stops requests where the
cookie does not contain that key-value pair (or it is false).

### Challenge 9

By adding the following node to the form:

    <input type="checkbox" name="files[]" value="2">

And then checking that box and downloading, we get flag.txt. This works since the web server does not seem to validate the values that are valid.

Value: realsec{zip_it_i_own_this_file}

## IFC

### Step 1

    l = h;

We can directly assign l to the value of h, since there is no limitation.

Code: codfish

### Step 2

    if (h) {
      l = true;
    } else {
      l = false;
    }

We can indirectly assign l to the value of h using an if statement, which conditionally sets the value of l based on h.

Code: joystick

### Step 3

    hatch = h;
    l = declassify(hatch);

The escape hatch allows us to assign the value of h to it, and then assign l to the value of hatch through declassify.

Code: graphite

### Step 4

    let (x = h) in l = x;

By writing the value of h to x, we can use x to assign l. Going around the direct assignment rule. 

Code: allergy

### Step 5

    l = true;
    try {
      if (h) {
          throw;
      }
      l = false;
    } catch {
      skip;
    }

Through using throw, we can conditionally assign l based on the value of h, without doing it in the catch block. 

Code: collect
