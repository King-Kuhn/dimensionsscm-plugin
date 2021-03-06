/*
 * ===========================================================================
 *  Copyright (c) 2007, 2014 Serena Software. All rights reserved.
 *
 *  Use of the Sample Code provided by Serena is governed by the following
 *  terms and conditions. By using the Sample Code, you agree to be bound by
 *  the terms contained herein. If you do not agree to the terms herein, do
 *  not install, copy, or use the Sample Code.
 *
 *  1.  GRANT OF LICENSE.  Subject to the terms and conditions herein, you
 *  shall have the nonexclusive, nontransferable right to use the Sample Code
 *  for the sole purpose of developing applications for use solely with the
 *  Serena software product(s) that you have licensed separately from Serena.
 *  Such applications shall be for your internal use only.  You further agree
 *  that you will not: (a) sell, market, or distribute any copies of the
 *  Sample Code or any derivatives or components thereof; (b) use the Sample
 *  Code or any derivatives thereof for any commercial purpose; or (c) assign
 *  or transfer rights to the Sample Code or any derivatives thereof.
 *
 *  2.  DISCLAIMER OF WARRANTIES.  TO THE MAXIMUM EXTENT PERMITTED BY
 *  APPLICABLE LAW, SERENA PROVIDES THE SAMPLE CODE AS IS AND WITH ALL
 *  FAULTS, AND HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EITHER
 *  EXPRESSED, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY
 *  IMPLIED WARRANTIES OR CONDITIONS OF MERCHANTABILITY, OF FITNESS FOR A
 *  PARTICULAR PURPOSE, OF LACK OF VIRUSES, OF RESULTS, AND OF LACK OF
 *  NEGLIGENCE OR LACK OF WORKMANLIKE EFFORT, CONDITION OF TITLE, QUIET
 *  ENJOYMENT, OR NON-INFRINGEMENT.  THE ENTIRE RISK AS TO THE QUALITY OF
 *  OR ARISING OUT OF USE OR PERFORMANCE OF THE SAMPLE CODE, IF ANY,
 *  REMAINS WITH YOU.
 *
 *  3.  EXCLUSION OF DAMAGES.  TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE
 *  LAW, YOU AGREE THAT IN CONSIDERATION FOR RECEIVING THE SAMPLE CODE AT NO
 *  CHARGE TO YOU, SERENA SHALL NOT BE LIABLE FOR ANY DAMAGES WHATSOEVER,
 *  INCLUDING BUT NOT LIMITED TO DIRECT, SPECIAL, INCIDENTAL, INDIRECT, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, DAMAGES FOR LOSS OF
 *  PROFITS OR CONFIDENTIAL OR OTHER INFORMATION, FOR BUSINESS INTERRUPTION,
 *  FOR PERSONAL INJURY, FOR LOSS OF PRIVACY, FOR NEGLIGENCE, AND FOR ANY
 *  OTHER LOSS WHATSOEVER) ARISING OUT OF OR IN ANY WAY RELATED TO THE USE
 *  OF OR INABILITY TO USE THE SAMPLE CODE, EVEN IN THE EVENT OF THE FAULT,
 *  TORT (INCLUDING NEGLIGENCE), STRICT LIABILITY, OR BREACH OF CONTRACT,
 *  EVEN IF SERENA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.  THE
 *  FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE
 *  MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW.  NOTWITHSTANDING THE ABOVE,
 *  IN NO EVENT SHALL SERENA'S LIABILITY UNDER THIS AGREEMENT OR WITH RESPECT
 *  TO YOUR USE OF THE SAMPLE CODE AND DERIVATIVES THEREOF EXCEED US$10.00.
 *
 *  4.  INDEMNIFICATION. You hereby agree to defend, indemnify and hold
 *  harmless Serena from and against any and all liability, loss or claim
 *  arising from this agreement or from (i) your license of, use of or
 *  reliance upon the Sample Code or any related documentation or materials,
 *  or (ii) your development, use or reliance upon any application or
 *  derivative work created from the Sample Code.
 *
 *  5.  TERMINATION OF THE LICENSE.  This agreement and the underlying
 *  license granted hereby shall terminate if and when your license to the
 *  applicable Serena software product terminates or if you breach any terms
 *  and conditions of this agreement.
 *
 *  6.  CONFIDENTIALITY.  The Sample Code and all information relating to the
 *  Sample Code (collectively "Confidential Information") are the
 *  confidential information of Serena.  You agree to maintain the
 *  Confidential Information in strict confidence for Serena.  You agree not
 *  to disclose or duplicate, nor allow to be disclosed or duplicated, any
 *  Confidential Information, in whole or in part, except as permitted in
 *  this Agreement.  You shall take all reasonable steps necessary to ensure
 *  that the Confidential Information is not made available or disclosed by
 *  you or by your employees to any other person, firm, or corporation.  You
 *  agree that all authorized persons having access to the Confidential
 *  Information shall observe and perform under this nondisclosure covenant.
 *  You agree to immediately notify Serena of any unauthorized access to or
 *  possession of the Confidential Information.
 *
 *  7.  AFFILIATES.  Serena as used herein shall refer to Serena Software,
 *  Inc. and its affiliates.  An entity shall be considered to be an
 *  affiliate of Serena if it is an entity that controls, is controlled by,
 *  or is under common control with Serena.
 *
 *  8.  GENERAL.  Title and full ownership rights to the Sample Code,
 *  including any derivative works shall remain with Serena.  If a court of
 *  competent jurisdiction holds any provision of this agreement illegal or
 *  otherwise unenforceable, that provision shall be severed and the
 *  remainder of the agreement shall remain in full force and effect.
 * ===========================================================================
 */
package hudson.plugins.dimensionsscm;

import com.serena.dmclient.api.DimensionsResult;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.VariableResolver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Deliver files to Dimensions CM repository using Java API.
 * The Jenkins Dimensions Plugin provides support for Dimensions CM SCM repositories.
 * @author Tim Payne
 */
public class CheckInAPITask extends GenericAPITask {
    private final boolean isForceTip;
    private final boolean isForceCheckIn;
    private final VariableResolver<String> myResolver;

    private final int buildNo;

    private final String jobId;

    private final String projectId;
    private final String owningPart;

    private final String[] patterns;
    private final String[] patternsExc;
    private final String patternType;

    public CheckInAPITask(AbstractBuild<?, ?> build, DimensionsSCM parent, int buildNo, String jobId, int version,
            ArtifactUploader artifact, FilePath workspace, TaskListener listener) {
        super(parent, workspace, listener);
        Logger.debug("Creating task - " + this.getClass().getName());

        // Config details
        this.projectId = parent.getProjectName(build, listener);
        this.isForceCheckIn = artifact.isForceCheckIn();
        this.isForceTip = artifact.isForceTip();
        this.owningPart = artifact.getOwningPart();
        this.patterns = artifact.getPatterns();
        this.patternsExc = artifact.getPatternsExc();

        this.patternType = artifact.getPatternType();

        // Build details.
        this.myResolver = build.getBuildVariableResolver();
        this.buildNo = buildNo;
        this.jobId = jobId;
    }

    @Override
    public Boolean execute(File area, VirtualChannel channel) throws IOException {
        boolean bRet = true;

        try {
            listener.getLogger().println("[DIMENSIONS] Scanning workspace for files to be saved into Dimensions...");
            listener.getLogger().flush();
            FilePath wd = new FilePath(area);
            Logger.debug("Scanning directory for files that match patterns '" + wd.getRemote() + "'");
            File dir = new File(wd.getRemote());

            File[] validFiles = new File[0];

            if (patternType.equals("regEx")) {
                listener.getLogger().println("[DIMENSIONS] Running RegEx pattern scanner...");
                FileScanner fs = new FileScanner(dir, patterns, patternsExc, -1);
                validFiles = fs.toArray();
                listener.getLogger().println("[DIMENSIONS] Found " + validFiles.length + " file(s) to check in...");
            } else if (patternType.equals("Ant")) {
                listener.getLogger().println("[DIMENSIONS] Running Ant pattern scanner...");
                FileAntScanner fs = new FileAntScanner(dir, patterns, patternsExc, -1);
                validFiles = fs.toArray();
                listener.getLogger().println("[DIMENSIONS] Found " + validFiles.length + " file(s) to check in...");
            }

            listener.getLogger().flush();

            if (validFiles.length > 0) {
                listener.getLogger().println("[DIMENSIONS] Loading files into Dimensions project \"" + projectId +
                        "\"...");
                listener.getLogger().flush();

                PrintWriter fmtWriter = null;
                File tmpFile = null;

                try {
                    tmpFile = File.createTempFile("dmCm" + Long.toString(System.currentTimeMillis()), null, null);
                    // 'DELIVER/USER_FILELIST=' user filelist in platform-default encoding.
                    fmtWriter = new PrintWriter(new FileWriter(tmpFile), true);

                    for (File f : validFiles) {
                        if (f.isDirectory()) {
                        } else {
                            Logger.debug("Found file '"+ f.getAbsolutePath() + "'");
                            fmtWriter.println(f.getAbsolutePath());
                        }
                    }
                    fmtWriter.flush();
                } catch (IOException e) {
                    bRet = false;
                    throw (IOException) new IOException(Values.exceptionMessage("Unable to write user filelist: " + tmpFile, e,
                            "no message")).initCause(e);
                } finally {
                    if (fmtWriter != null) {
                        fmtWriter.close();
                    }
                }

                // Debug for printing out files
                //String filesToLoad = new String(FileUtils.loadFile(tmpFile));
                //if (filesToLoad != null) {
                //    filesToLoad += "\n";
                //    filesToLoad = filesToLoad.replaceAll("\n\n", "\n");
                //    listener.getLogger().println(filesToLoad.replaceAll("\n", "\n[DIMENSIONS] - "));
                //}

                {
                    String requests = myResolver.resolve("DM_TARGET_REQUEST");

                    if (requests != null) {
                        requests = requests.replaceAll(" ", "");
                        requests = requests.toUpperCase(Values.ROOT_LOCALE);
                    }

                    DimensionsResult res = dmSCM.UploadFiles(key, wd, projectId, tmpFile, jobId, buildNo, requests,
                            isForceCheckIn, isForceTip, owningPart);
                    if (res == null) {
                        listener.getLogger().println("[DIMENSIONS] New artifacts failed to get loaded into Dimensions");
                        listener.getLogger().flush();
                        bRet = false;
                    } else {
                        listener.getLogger().println("[DIMENSIONS] Build artifacts were successfully loaded into Dimensions");
                        listener.getLogger().println("[DIMENSIONS] (" + res.getMessage().replaceAll("\n",
                                "\n[DIMENSIONS] ") + ")");
                        listener.getLogger().flush();
                    }
                }

                if (tmpFile != null) {
                    tmpFile.delete();
                } else {
                    listener.getLogger().println("[DIMENSIONS] No build artifacts were detected");
                    listener.getLogger().flush();
                }
            } else {
                listener.getLogger().println("[DIMENSIONS] No build artifacts found for checking in");
            }
            listener.getLogger().flush();
        } catch (Exception e) {
            listener.fatalError(Values.exceptionMessage("Unable to run checkin callout", e, "no message - try again"));
            bRet = false;
        }
        return bRet;
    }
}
