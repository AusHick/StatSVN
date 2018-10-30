StatSVN
------------
StatSVN is a statistics tool for SVN repositories. It generates HTML reports from SVN log files.

#### The StatSVN Manual
The StatSVN manual is located here: http://svn.statsvn.org/statsvnwiki/index.php/UserManual

#### Quick Start
1. Download the latest release from [here](https://github.com/Revenge282/StatSVN/releases/latest "here").
2. Expand the zip file into some directory, e.g C:\statsvn
3. Check out a working copy of the desired SVN module into some directory, e.g. C:\myproject.
4. Change into that directory and type `svn log --xml -v > svn.log`
5. Change back to the c:\statsvn directory
6. Type `java -jar statsvn.jar C:\myproject\svn.log C:\myproject`
7. Open C:\statsvn\index.html in your web browser

You can tweak the output of StatSVN in various ways. Run `java -jar statsvn.jar` for an overview of the command line parameters, and check the manual for full information.