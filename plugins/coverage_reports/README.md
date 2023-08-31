# Additional JCov reports

This module contains source code implementing varions code coverage reporting
aproaches.

Currently implemented report formats:
1. Single-file html.
2. Text.

Currently implemented reports:
1. Diff coverage report. See JDKReport for an example of usage. Next input needs to be provided: 
   1. Coverage data in form of JCov file
   2. Diff file (only output of git diff has been tested so far)
   3. Source code
   
