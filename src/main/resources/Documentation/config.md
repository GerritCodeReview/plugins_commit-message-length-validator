Commit Message Length Configuration
===================================

The maximum lengths of the subject and message body can be
configured in the standard Gerrit config file `gerrit.config`.

commitmessage.maxSubjectLength
:	Maximum length of the commit message's subject line.  Defaults
	to 65 if not specified or less than 0.

commitmessage.maxLineLength
:	Maximum length of a line in the commit message's body.  Defaults
	to 70 if not specified or less than 0.

commitmessage.longLinesThreshold
:	Percentage of commit message lines allowed to exceed the
	maximum length before a warning or error is generated.  Defaults
	to 33 if not specified or less than 0.

commitmessage.rejectTooLong
:	If set to `true`, reject commits whose subject or line
	length exceeds the maximum allowed length.  If not
	specified, defaults to `false`.

commitmessage.rejectNoMsgBody
:	If set to `true`, reject commits that do not contain a commit
    message body or description.  If not specified, defaults
    to `false`.
