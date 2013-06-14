package com.jdon.mvc.flow;

import com.jdon.mvc.core.Env;
import com.jdon.mvc.core.FlowContext;
import com.jdon.mvc.http.FormFile;
import com.jdon.mvc.http.FormFileImp;
import com.jdon.mvc.util.StringUtils;
import com.jdon.mvc.util.WebUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 文件上传解析拦截
 * <p/>
 * todo 没有清除临时文件
 *
 * @author oojdon
 */

public class FormFileFlowUnit implements FlowUnit {

    private final Log LOG = LogFactory.getLog(getClass());

    private final static long sizeLimit = 2 * 1024 * 1024;

    private List<FileItem> fileItems;


    public void process(FlowContext context) {


        if (ServletFileUpload.isMultipartContent(Env.req())) {
            DiskFileItemFactory factory = createFactoryForDiskBasedFileItems();
            ServletFileUpload fileUploadHandler = new ServletFileUpload(factory);
            fileUploadHandler.setSizeMax(sizeLimit);
            try {
                fileItems = fileUploadHandler.parseRequest(Env.req());
            } catch (FileUploadException e) {
                LOG.warn("There was some problem parsing this multipart Env.req(), or someone is not sending a RFC1867 compatible multipart Env.req().", e);
            }
            LOG.debug("Found [" + fileItems.size() + "] attributes in the multipart form submission. Parsing them.");
            parseFileItem();
        }

        LOG.info("the flow-unit of " + FormFileFlowUnit.class.getName() + " finish");

    }


    private void parseFileItem() {
        for (FileItem item : fileItems) {
            if (item.isFormField()) {
                String encoding = Env.req().getCharacterEncoding();

                try {
                    if (encoding == null)
                        Env.req().addParameterValue(item.getFieldName(), item.getString("ISO-8859-1"));
                    else
                        Env.req().addParameterValue(item.getFieldName(), item.getString(encoding));
                } catch (UnsupportedEncodingException e) {
                    LOG.warn("can't supported the encoding,please check your filter" + e);
                }
            } else {
                if (StringUtils.isNotEmpty(item.getName())) {
                    try {
                        FormFile formFile = new FormFileImp(item);
                        Env.req().addFormFile(item.getFieldName(), formFile);
                        LOG.info("Uploaded file: " + item.getFieldName() + " with " + formFile);
                    } catch (Exception e) {
                        LOG.error("Nasty uploaded file " + item.getName(), e);
                    }
                } else {
                    LOG.info("A file field was empty: " + item.getFieldName());
                }
            }
        }

    }


    private DiskFileItemFactory createFactoryForDiskBasedFileItems() {
        DiskFileItemFactory factory = new DiskFileItemFactory(4096 * 16, WebUtils.getTempDir(Env.ctx()));
        LOG.debug("Using repository [" + factory.getRepository() + "]");
        return factory;
    }

}
