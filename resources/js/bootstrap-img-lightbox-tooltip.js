(function($) {

    $.fn.extend({
        img_lightbox_tooltip: function(options) {
            options = $.extend( {}, $.MyFunc.defaults, options );

            this.each(function() {
                new $.MyFunc(this,options);
            });
            return this;
        }
    });

    // ctl is the element, options is the set of defaults + user options
    $.MyFunc = function( ctl, options ) {
			if ( $(ctl).attr("src") ) {
				var imgsrc = $(ctl).attr("src")
				$(ctl).wrap("<div style='pointer:cursor'>")
				var that = $(ctl).parent()
				$(that).click(function() {
					$(".lightbox-content img").attr("src", imgsrc)
					$("#demoLightbox").lightbox()
				})
				$(that).tooltip({ placement: options.placement, title : options.title})
				if (options.tooltip_show == "always") { 
					$(that).tooltip('show')
				}
			}
    };

    // option defaults
    $.MyFunc.defaults = {
        //...hash of default settings...
		placement : "top", // top | bottom | right | left
		tooltip_show : "always", // always | hover
		title : "click to enlarge"
    };

})(jQuery);

