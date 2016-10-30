
$(function(){
	
	$('.admin-nav > li').on('click',function(){
		var $this = $(this);
		if($this.hasClass('active'))return;
		$this.addClass('active').siblings().removeClass('active');
	});
	
	$(".admin-nav a").on("click",function(){
		var loadUrl = $(this).attr('pagekey') + "?_t="+new Date().getTime();
		$('#maincontents').load(loadUrl, function() {
			registerEvent();
		});
	});
	
});

function registerEvent(){
	$(".collapse .panel-head").each(function() {
        var e = $(this);
        e.click(function() {
            e.closest(".collapse").find(".panel").removeClass("active");
            e.closest(".panel").addClass("active")
        })
    });
    $(".collapse-toggle .panel-head").each(function() {
        var e = $(this);
        e.click(function() {
            e.closest(".panel").toggleClass("active")
        })
    });
}