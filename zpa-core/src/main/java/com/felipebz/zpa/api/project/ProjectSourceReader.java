package com.felipebz.zpa.api.project;

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi;

/**
 * Caller-owned source acquisition used only during synchronous project preparation.
 * Each source invokes its reader at most once per preparation; concurrent preparation
 * may invoke readers on worker threads. Shared captured state must support that concurrency. The caller owns
 * decoding and resource closure. Neither this callback nor its captures enter the snapshot.
 */
@ZpaExperimentalApi
@FunctionalInterface
public interface ProjectSourceReader {
    /** Returns non-null source text; an Exception becomes a per-file failure summary. */
    String read() throws Exception;
}
