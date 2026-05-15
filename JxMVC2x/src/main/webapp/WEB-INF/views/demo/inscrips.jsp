<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://jxmvc/tags" prefix="jx" %>

<%@ page import="jxmvc.core.*" %>


<%@ include file="/WEB-INF/views/shared/header.jspf" %>

<section class="py-10">
    <div class="max-w-3xl">
        <p class="text-sm uppercase tracking-[0.35em] text-slate-500 dark:text-slate-400">BD Demo</p>
        <h1 class="text-4xl md:text-5xl font-semibold mt-3">Listado desde DBRow</h1>
        <p class="text-lg text-slate-600 dark:text-slate-300 mt-4">
            Ejemplo simple usando DBRow + JxTagFor/JxTagIf.
        </p>
    </div>

    <div class="bg-white/90 dark:bg-slate-900/70 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-lg mt-10">
        <p class="text-xs uppercase tracking-[0.3em] text-slate-400">Estado conexión</p>
        <p class="text-sm text-slate-600 dark:text-slate-300 mt-2">${connState}</p>
        <jx:if test="${hasRows}">
            <div class="overflow-x-auto">
                <table class="w-full text-sm">
                    <thead>
                        <tr class="text-left text-xs uppercase tracking-[0.3em] text-slate-400">
                            <th class="py-3 px-4">ID</th>
                            <th class="py-3 px-4">Nombre</th>
                            <th class="py-3 px-4">Rol</th>
                            <th class="py-3 px-4">Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        
                        <jx:for var="row" items="${ tbl.Result() }" maxCount="10">
                            <tr class="border-t border-slate-200 dark:border-slate-800">
                                <td class="py-3 px-4 font-semibold">${row.get("id")}</td>
                                <td class="py-3 px-4">${row.get("numDoc")}</td>
                                <td class="py-3 px-4">${row.get("Correo")}</td>
                                <td class="py-3 px-4 text-slate-500 dark:text-slate-400">Activo</td>
                            </tr>
                        </jx:for>
                    </tbody>
                </table>
            </div>
        </jx:if>
        <jx:if test="${!hasRows}">
            <p class="text-sm text-slate-500 dark:text-slate-400">Sin registros.</p>
        </jx:if>
    </div>
</section>

<%@ include file="/WEB-INF/views/shared/footer.jspf" %>
