
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="py-10">
    <div class="max-w-3xl">
        <p class="text-sm uppercase tracking-[0.35em] text-slate-500 dark:text-slate-400">Upload</p>
        <h1 class="text-4xl md:text-5xl font-semibold mt-3">Subida de archivo</h1>
        <p class="text-lg text-slate-600 dark:text-slate-300 mt-4">
            Demo usando JxRequest.UploadFile.
        </p>
    </div>

    <div class="bg-white/90 dark:bg-[#111111]/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg mt-10">
        <form method="post" action="${pageContext.request.contextPath}/demo/upload" enctype="multipart/form-data" class="space-y-4">
            <input type="file" name="file" class="block w-full text-sm" />
            <button type="submit" class="px-5 py-2.5 rounded-full bg-ink text-white dark:bg-white dark:text-slate-900 text-sm font-medium">
                Subir
            </button>
        </form>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-4">Destino: /tmp/uploads</p>
    </div>
</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>
