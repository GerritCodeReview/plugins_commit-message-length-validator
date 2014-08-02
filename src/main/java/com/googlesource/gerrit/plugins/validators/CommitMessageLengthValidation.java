// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.validators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Listen
@Singleton
public class CommitMessageLengthValidation implements CommitValidationListener {
  private final static int DEFAULT_MAX_SUBJECT_LENGTH = 65;
  private final static int DEFAULT_MAX_LINE_LENGTH = 70;
  private final static int DEFAULT_LONG_LINES_THRESHOLD = 33;
  private final static RejectTooLong DEFAULT_REJECT_TOO_LONG = RejectTooLong.FALSE;
  private final static String COMMIT_MESSAGE_SECTION = "commitmessage";
  private final static String MAX_SUBJECT_LENGTH_KEY = "maxSubjectLength";
  private final static String MAX_LINE_LENGTH_KEY = "maxLineLength";
  private final static String REJECT_TOO_LONG_KEY = "rejectTooLong";
  private final static String LONG_LINES_THRESHOLD_KEY = "longLinesThreshold";

  private final Config config;
  private final int maxSubjectLength;
  private final int maxLineLength;
  private final int longLinesThreshold;
  private RejectTooLong rejectTooLong;

  @Inject
  public CommitMessageLengthValidation(@GerritServerConfig Config gerritConfig)
      throws ConfigInvalidException, IOException {
    this.config = gerritConfig;
    this.maxSubjectLength = config.getInt(
        COMMIT_MESSAGE_SECTION, null,
        MAX_SUBJECT_LENGTH_KEY, DEFAULT_MAX_SUBJECT_LENGTH);
    this.maxLineLength = config.getInt(
        COMMIT_MESSAGE_SECTION, null,
        MAX_LINE_LENGTH_KEY, DEFAULT_MAX_LINE_LENGTH);
    this.rejectTooLong = config.getEnum(
        COMMIT_MESSAGE_SECTION, null,
        REJECT_TOO_LONG_KEY, DEFAULT_REJECT_TOO_LONG);
    this.longLinesThreshold = config.getInt(
        COMMIT_MESSAGE_SECTION, null,
        LONG_LINES_THRESHOLD_KEY, DEFAULT_LONG_LINES_THRESHOLD);
  }

  private void onLineTooLong(final AbbreviatedObjectId id, String refName,
      List<CommitValidationMessage> messagesList, final String errorMessage)
          throws CommitValidationException {
    final String message = id.name() + ": " + errorMessage;
    if (rejectTooLong == RejectTooLong.TRUE
        || (rejectTooLong == RejectTooLong.FOR_REVIEW && !refName
            .startsWith("refs/heads/"))) {
      messagesList.add(new CommitValidationMessage(message, true));
      throw new CommitValidationException("Commit length validation failed", messagesList);
    } else {
      messagesList.add(new CommitValidationMessage("(W) " + message, false));
    }
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    final RevCommit commit = receiveEvent.commit;
    final AbbreviatedObjectId id = commit.abbreviate(7);
    String refName = receiveEvent.command.getRefName();
    List<CommitValidationMessage> messages = new ArrayList<CommitValidationMessage>();

    if (this.maxSubjectLength < commit.getShortMessage().length()) {
      onLineTooLong(id, refName, messages,
          new String("commit subject >" + this.maxSubjectLength
              + " characters; use shorter first paragraph"));
    }

    int longLineCnt = 0, nonEmptyCnt = 0;
    for (String line : commit.getFullMessage().split("\n")) {
      if (!line.trim().isEmpty()) {
        nonEmptyCnt++;
      }
      if (this.maxLineLength < line.length()) {
        longLineCnt++;
      }
    }

    if (longLineCnt > (longLinesThreshold * nonEmptyCnt) / 100) {
      onLineTooLong(id, refName, messages,
          new String("too many commit message lines longer than "
              + this.maxLineLength
              + " characters; manually wrap lines"));
    }

    return messages;
  }
}
