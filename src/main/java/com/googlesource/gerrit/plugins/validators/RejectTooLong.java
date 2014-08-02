package com.googlesource.gerrit.plugins.validators;

/**
 * Enumeration for different options of rejecting message length limit exceeding
 * commits.
 */
public enum RejectTooLong {
  /** Reject any commit that has too long message. */
  TRUE,

  /**
   * Reject commits that have too long messages. But don't reject commits on
   * <code>refs/heads/*</code>.
   */
  FOR_REVIEW,

  /** Don't reject any commit - just print warnings. */
  FALSE;
}
