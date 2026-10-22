<#import "footer.ftl" as loginFooter>
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${lang}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="robots" content="noindex, nofollow" />

    <title>${title!}</title>

    <#if properties.odaLogoUrl?has_content>
        <link rel="icon" href="${properties.odaLogoUrl}" />
    <#else>
        <link rel="icon" href="${url.resourcesPath}/img/logo.svg" />
    </#if>

    <#-- Brand colors injected from theme.properties (env-configurable) -->
    <style>
        :root {
            --oda-primary: ${properties.odaPrimaryColor!'oklch(0.5635 0.2408 260.8178)'};
            --oda-primary-foreground: ${properties.odaPrimaryForeground!'#ffffff'};
            --oda-accent: ${properties.odaAccentColor!'oklch(0.6489 0.2370 26.9728)'};
        }
    </style>

    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>

    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>

    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>
<body class="${properties.kcBodyClass!}" data-page-id="login-${pageId}">
<div class="${properties.kcLoginClass!}">
    <div class="oda-login__inner">
        <#-- Header / logo -->
        <header class="${properties.kcHeaderClass!}">
            <div class="${properties.kcHeaderWrapperClass!}">
                <#if properties.odaLogoUrl?has_content>
                    <img class="oda-logo" src="${properties.odaLogoUrl}" alt="${properties.odaBrandName!}" />
                <#else>
                    <img class="oda-logo" src="${url.resourcesPath}/img/logo.svg" alt="${properties.odaBrandName!}" />
                </#if>
                <#if properties.odaBrandTagline?has_content>
                    <p class="oda-tagline">${properties.odaBrandTagline}</p>
                </#if>
            </div>
        </header>

        <div class="${properties.kcFormCardClass!}">
            <header class="${properties.kcFormHeaderClass!}">
                <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                    <div class="oda-locale">
                        <#list locale.supported as l>
                            <a href="${l.url}" class="oda-locale__link">${l.label}</a>
                        </#list>
                    </div>
                </#if>
                <h1 id="kc-page-title" class="oda-card__title"><#nested "header"></h1>
            </header>

            <div id="kc-content">
                <div id="kc-content-wrapper">
                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="oda-alert oda-alert--${message.type}">
                            <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#nested "form">

                    <#if auth?has_content && auth.showTryAnotherWayLink()>
                        <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                            <input type="hidden" name="tryAnotherWay" value="on"/>
                            <a href="#" id="try-another-way" onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                        </form>
                    </#if>

                    <#nested "socialProviders">

                    <#if displayInfo>
                        <div id="kc-info" class="${properties.kcSignUpClass!}">
                            <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                                <#nested "info">
                            </div>
                        </div>
                    </#if>
                </div>
            </div>

            <@loginFooter.content/>
        </div>
    </div>
</div>
</body>
</html>
</#macro>
