Commit Message Length Configuration
===================================

The maximum lengths of the subject and message body can be
configured in the standard Gerrit config file `gerrit.config`.

commitmessage.maxSubjectLength
:	Maximum length of the commit message's subject line.  If
	not specified, defaults to 65.

commitmessage.maxLineLength
:	Maximum length of a line in the commit message's body.  If
	not specified, defaults to 70.

commitmessage.longLinesThreshold
:	Percentage of commit message lines allowed to exceed the
	maximum length before a warning or error is generated.  If
	not specified, defaults to 33.

commitmessage.rejectTooLong
:	If set to `true`, reject commits whose subject or line
	length exceeds the maximum allowed length.  If not
	specified, defaults to `false`.
