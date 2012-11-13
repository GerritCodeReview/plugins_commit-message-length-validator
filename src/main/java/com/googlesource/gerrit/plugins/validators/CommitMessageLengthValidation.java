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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Singleton;

@Listen
@Singleton
public class CommitMessageLengthValidation implements CommitValidationListener {

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    final RevCommit commit = receiveEvent.commit;
    final AbbreviatedObjectId id = commit.abbreviate(7);
    List<CommitValidationMessage> messages = new ArrayList<CommitValidationMessage>();

    if (65 < commit.getShortMessage().length()) {
      messages.add(new CommitValidationMessage("(W) " + id.name() //
         + ": commit subject >65 characters; use shorter first paragraph", false));
    }

    int longLineCnt = 0, nonEmptyCnt = 0;
    for (String line : commit.getFullMessage().split("\n")) {
      if (!line.trim().isEmpty()) {
        nonEmptyCnt++;
      }
      if (70 < line.length()) {
        longLineCnt++;
      }
    }

    if (0 < longLineCnt && 33 < longLineCnt * 100 / nonEmptyCnt) {
      messages.add(new CommitValidationMessage("(W) " + id.name() //
          + ": commit message lines >70 characters; manually wrap lines", false));
    }

    return messages;
  }
}
