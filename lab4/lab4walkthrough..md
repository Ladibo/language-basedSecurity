DatabaseActivity.java
data endpoint

### MainActivity.addItem
Section 4 of the paper, reading /proc/net/arp and /proc/net/wireless contains BSSID (the MAC address of the Wi-Fi access point you're connected to)

PS C:\Users\David\Documents\lbs-android-main\unsolved\MACLocation> adb shell
generic_x86_64:/ $ cat /proc/net/arp
IP address       HW type     Flags       HW address            Mask     Device
192.168.232.1    0x1         0x2         02:00:00:00:01:00     *        wlan0
192.168.200.1    0x1         0x2         1a:cb:0f:ab:95:ce     *        radio0

HW is MAC adress, aka the BSSID. This is the value the paper says you can feed to Skyhook/Navizon/Google's BSSID database to physically locate the device. So on a real device MAC would be the physical router connected on wifi.
radio0 is something from the Android emulator
So even here if we have zero permissions we can still pull out the adress

generic_x86_64:/ $ cat /proc/net/wireless
cat: /proc/net/wireless: No such file or directory
1|generic_x86_64:/ $ ls /sys/class/net/
hwsim0 ip6_vti0 ip6tnl0 ip_vti0 lo radio0 sit0 wlan0 

Item.java
Title and info. Set the 

## MainActivity.onYes

Section 2.2 Networking in the paper on Leviathans blog. Describes how to smuggle data out through internet with zero-permission app.