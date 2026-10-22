<#macro content>
<footer class="oda-footer">
    <ul class="oda-footer__links">
        <#if properties.odaHomeUrl?has_content>
            <li><a href="${properties.odaHomeUrl}">${msg("odaHome")}</a></li>
        </#if>
        <#if properties.odaPrivacyUrl?has_content>
            <li><a href="${properties.odaPrivacyUrl}">${msg("odaPrivacy")}</a></li>
        </#if>
        <#if properties.odaTermsUrl?has_content>
            <li><a href="${properties.odaTermsUrl}">${msg("odaTerms")}</a></li>
        </#if>
        <#if properties.odaSupportEmail?has_content>
            <li><a href="mailto:${properties.odaSupportEmail}">${msg("odaSupport")}</a></li>
        </#if>
    </ul>
</footer>
</#macro>
