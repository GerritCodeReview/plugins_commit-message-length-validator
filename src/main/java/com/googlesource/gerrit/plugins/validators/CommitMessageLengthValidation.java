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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Listen
@Singleton
public class CommitMessageLengthValidation implements CommitValidationListener {
  private static Logger log = LoggerFactory.getLogger(CommitValidationListener.class);

  private final static int DEFAULT_MAX_SUBJECT_LENGTH = 65;
  private final static int DEFAULT_MAX_LINE_LENGTH = 70;
  private final static String CONFIG_FILE = "commitmessage.config";
  private final static String COMMIT_MESSAGE_SECTION = "commitmessage";
  private final static String MAX_SUBJECT_LENGTH_KEY = "maxSubjectLength";
  private final static String MAX_LINE_LENGTH_KEY = "maxLineLength";

  private int maxSubjectLength;
  private int maxLineLength;

  @Inject
  public CommitMessageLengthValidation(final SitePaths site)
      throws ConfigInvalidException, IOException {
    final File cfgPath = new File(site.etc_dir, CONFIG_FILE);
    if (!cfgPath.exists() || cfgPath.length() == 0) {
      log.warn("Config file " + cfgPath + " does not exist or is empty; using default values");
    } else {
      final FileBasedConfig cfg = new FileBasedConfig(cfgPath, FS.DETECTED);
      try {
        cfg.load();
        this.maxSubjectLength = cfg.getInt(COMMIT_MESSAGE_SECTION, MAX_SUBJECT_LENGTH_KEY,
            DEFAULT_MAX_SUBJECT_LENGTH);
        this.maxLineLength = cfg.getInt(COMMIT_MESSAGE_SECTION, MAX_LINE_LENGTH_KEY,
            DEFAULT_MAX_LINE_LENGTH);
        return;
      } catch (ConfigInvalidException e) {
        throw new ConfigInvalidException(String.format(
            "Config file %s is invalid: %s", cfgPath, e.getMessage()), e);
      } catch (IOException e) {
        throw new IOException(String.format(
            "Cannot read %s: %s", cfgPath,  e.getMessage()), e);
      }
    }
    this.maxSubjectLength = DEFAULT_MAX_SUBJECT_LENGTH;
    this.maxLineLength = DEFAULT_MAX_LINE_LENGTH;
  }


  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    final RevCommit commit = receiveEvent.commit;
    final AbbreviatedObjectId id = commit.abbreviate(7);
    List<CommitValidationMessage> messages = new ArrayList<CommitValidationMessage>();

    if (this.maxSubjectLength < commit.getShortMessage().length()) {
      messages.add(new CommitValidationMessage("(W) " + id.name() //
         + ": commit subject >" + this.maxSubjectLength //
         + " characters; use shorter first paragraph", false));
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

    if (0 < longLineCnt && 33 < longLineCnt * 100 / nonEmptyCnt) {
      messages.add(new CommitValidationMessage("(W) " + id.name() //
          + ": commit message lines >" + this.maxLineLength //
          + " characters; manually wrap lines", false));
    }

    return messages;
  }
}
