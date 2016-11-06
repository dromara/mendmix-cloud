//拓展方法
;(function($) {
	$.extend($,{
		success : function(msg){
		  $.tip({content:msg, icon:'success'});
	    },
	    alert : function(msg){
	    	$.tip({content:msg, icon:'alert'});
	    },
		error : function(msg){
	    	$.tip({content:msg, icon:'error'});
	    }
	});
})(jQuery);
;(function($) {
	$.weiApp = $.weiApp || {version : "v1.0.0"};
	$.extend($.weiApp,{
		        isLogin : function(){
		        	if(loginUid <= 0){
		        		$.dialog({
		        			title:'登录',
		        			lock: true,
		        			opacity:0.5,
		        			//padding:'0px 0px',
		        		    content: document.getElementById('login_dialog'),
		        		    id: 'login_dialog'
		        		});
		        		return false;
		        	}
		        	return true;
		        },
		        successTips : function(msg){
		        	$.weiApp.tip({content:msg, icon:'success'});
		        },
                errorTips : function(msg){
                	$.weiApp.tip({content:msg, icon:'error'});
		        },
				utils : {
					       /*比较日期大小，大于返回正数，等于返回0*/
					       compareDate : function(fisrt, second,type) {
				    		   var array0 = fisrt.split(/[^0-9]/g);
				    			var array1 = second.split(/[^0-9]/g);
				    			var len = array0.length > array1.length ? array0.length : array1.length;
				    			for ( var i = 0; i < len; i++) {
				    				var temp0 = array0[i] | 0;
				    				var temp1 = array1[i] | 0;
				    				if (temp0 != temp1)
				    					return temp0 - temp1;
				    			}
				    			return 0;
					       },
					       isBlank:function(str){
					    	   return str == null ? true : str.replace(/[ ]/g,"") == "";
					       },
					       getShortStr:function(str,length){
					    	   return str.substr(0,length) + (str.length > length ? ".." : "");
					       },
							redirect : function(uri, toiframe) {
								if (toiframe != undefined) {
									$('#' + toiframe).attr('src', uri);
									return false;
								}
								location.href = uri;
							}
						}
					});
})(jQuery);


/**
 * 页面初始化通用方法
 */
$(document).ready(function(){
	$.commons.init();
}); 
;(function($){
	$.commons = {
			init:function(){
				$.commons.initCalendar();
				$.commons.initTab();
				$.commons.initTip();
				$.commons.initCollapsePanel();
				$.commons.initPromptEvent();
				$.commons.initDialogEvent();
				$.commons.initConfirmEvent();
				$.commons.initAjaxFormSubmit();
				$.commons.initToolBtn();
				$.commons.initPreviewImage();
				//ajax全局设置
				$.ajaxSetup({
				   cache: false,
				   global: true,
				   type: "POST",
				   dataType: "json"
				});
			},
			initTab: function(){
				$('.tab .tab-nav li').each(function(){
					var e=$(this);
					var trigger=e.closest('.tab').attr("data-toggle");
					if (trigger=="hover"){
						e.mouseover(function(){
							$showtabs(e);
						});
						e.click(function(){
							return false;
						});
					}else{
						e.click(function(){
							$showtabs(e);
							return false;
						});
					}
				});
				$showtabs=function(e){
					var detail=e.children("a").attr("href");
					e.closest('.tab .tab-nav').find("li").removeClass("active");
					e.closest('.tab').find(".tab-body .tab-panel").removeClass("active");
					e.addClass("active");
					$(detail).addClass("active");
				};
			},
			initCollapsePanel: function(){
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
			},
			initTip: function(){
				 $(".tips").each(function() {
				        var e = $(this);
				        var title = e.attr("title");
				        var trigger = e.attr("data-toggle");
				        e.attr("title", "");
				        if (trigger == "" || trigger == null) {
				            trigger = "hover"
				        }
				        if (trigger == "hover") {
				            e.mouseover(function() {
				                $showtips(e, title)
				            })
				        } else {
				            if (trigger == "click") {
				                e.click(function() {
				                    $showtips(e, title)
				                })
				            } else {
				                if (trigger == "show") {
				                    e.ready(function() {
				                        $showtips(e, title)
				                    })
				                }
				            }
				        }
				    });
				    $showtips = function(e, title) {
				        var trigger = e.attr("data-toggle");
				        var place = e.attr("data-place");
				        var width = e.attr("data-width");
				        var css = e.attr("data-style");
				        var image = e.attr("data-image");
				        var content = e.attr("content");
				        var getid = e.attr("data-target");
				        var data = e.attr("data-url");
				        var x = 0;
				        var y = 0;
				        var html = "";
				        var detail = "";
				        if (image != null) {
				            detail = detail + '<img class="image" src="' + image + '" />'
				        }
				        if (content != null) {
				            detail = detail + '<p class="tip-body">' + content + "</p>"
				        }
				        if (getid != null) {
				            detail = detail + $(getid).html()
				        }
				        if (data != null) {
				            detail = detail + $.ajax({
				                url: data,
				                async: false
				            }).responseText
				        }
				        if (title != null && title != "") {
				            if (detail != null && detail != "") {
				                detail = '<p class="tip-title"><strong>' + title + "</strong></p>" + detail
				            } else {
				                detail = '<p class="tip-line">' + title + "</p>"
				            }
				            e.attr("title", "")
				        }
				        detail = '<div class="tip">' + detail + "</div>";
				        html = $(detail);
				        $("body").append(html);
				        if (width != null) {
				            html.css("width", width)
				        }
				        if (place == "" || place == null) {
				            place = "top"
				        }
				        if (place == "left") {
				            x = e.offset().left - html.outerWidth() - 5;
				            y = e.offset().top - html.outerHeight() / 2 + e.outerHeight() / 2
				        } else {
				            if (place == "top") {
				                x = e.offset().left - html.outerWidth() / 2 + e.outerWidth() / 2;
				                y = e.offset().top - html.outerHeight() - 5
				            } else {
				                if (place == "right") {
				                    x = e.offset().left + e.outerWidth() + 5;
				                    y = e.offset().top - html.outerHeight() / 2 + e.outerHeight() / 2
				                } else {
				                    if (place == "bottom") {
				                        x = e.offset().left - html.outerWidth() / 2 + e.outerWidth() / 2;
				                        y = e.offset().top + e.outerHeight() + 5
				                    }
				                }
				            }
				        }
				        if (css != "") {
				            html.addClass(css)
				        }
				        html.css({
				            "left": x + "px",
				            "top": y + "px",
				            "position": "absolute"
				        });
				        if (trigger == "hover" || trigger == "click" || trigger == null) {
				            e.mouseout(function() {
				                html.remove();
				                e.attr("title", title)
				            })
				        }
				    };
			},
			//初始化日历
            initCalendar: function(){
				$(".date-input").each(function(i){
					var format = $(this).attr('format');
					if(!format || format == '')format = 'yyyy-MM-dd';
					$(this).calendar({ format:format});
				});
			},
			initPreviewImage: function(){
				$("img[data-component='preview']").each(function(i){
					var w = $(window).width();
					var h = $(window).height();
					$(this).hover(function(e){
						if(/.png$|.gif$|.jpg$|.bmp$|.jpeg$/.test($(this).attr("data-bimg"))){
							$("body").append("<div id='preview'><img src='"+$(this).attr('data-bimg')+"' /></div>");
						}
						var show_x = $(this).offset().left + $(this).width();
						var show_y = $(this).offset().top;
						var scroll_y = $(window).scrollTop();
						$("#preview").css({
							position:"absolute",
							padding:"4px",
							border:"1px solid #f3f3f3",
							backgroundColor:"#eeeeee",
							top:show_y + "px",
							left:show_x + "px",
							zIndex:1000
						});
						$("#preview > div").css({
							padding:"5px",
							backgroundColor:"white",
							border:"1px solid #cccccc"
						});
						if (show_y + 230 > h + scroll_y) {
							$("#preview").css("bottom", h - show_y - $(this).height() + "px").css("top", "auto");
						} else {
							$("#preview").css("top", show_y + "px").css("bottom", "auto");
						}
						$("#preview").fadeIn("fast");
					},function(){
						$("#preview").remove();
					})					  
				});
			},
			//初始化弹出dialog事件
			initPromptEvent:function(){
				$('body').on('click','.J_prompt', function(){
					var self = $(this),
					    title = self.attr('data-title') || '请输入',
					    defaultVal = self.attr('data-val') || '',
					    formType = self.attr('data-formType') || 0,//0:input ,1: password,3:textarea
					    callback = self.attr('data-callback');
					layer.prompt({
					    title: title,
					    formType: formType 
					}, function(val,index){
						eval(callback+'(self,val)');
						layer.close(index);
					});
					$('.layui-layer-input').val(defaultVal);
				});
			},
			initDialogEvent:function(){
				//弹窗表单
				$('.J_showdialog').on('click', function(){
					var self = $(this),
						dtitle = self.attr('data-title') || '',
						dtarget = self.attr('data-target'),
						dwidth = self.attr('data-width') ? parseInt(self.attr('data-width')) :  600,
						dheight = self.attr('data-height') ? parseInt(self.attr('data-height')) : 450;

					//iframe   load  
					var type = 1,content,
					area = eval("['"+dwidth+"px', '"+dheight+"px']");
					if(dtarget.indexOf('load') >= 0){
						content = "<div id=\"innerCont\">Loading...</div>";
						var url = content = dtarget.split(":")[1];
						$.getJSON(url,function(html){
							$("#innerCont").html(html);
						});
					}else if(dtarget.indexOf('iframe') >= 0){
						type = 2;
						content = dtarget.split(":")[1];
					}else if(dtarget.substr(0,1) == '#' || dtarget.substr(0,1) == '.'){
						content = $(dtarget).html();
					}
					layer.open({
					    type: type,
					    title: dtitle,
					    shadeClose: true,
					    shade: 0.8,
					    area: area,
					    content: content 
					}); 
				});
			},
			//初始化确认提示事件
			initConfirmEvent : function(){
				$('body').on('click','.J_confirmurl', function(){
					var self = $(this),
						uri = self.attr('act-uri'),
						msg = self.attr('act-msg') || '您确认该操作吗',
						callback = self.attr('act-callback');
					
					layer.confirm(msg, {
					    btn: ['确定','取消'], //按钮
					    shade: false //不显示遮罩
					}, function(index){
				        $.getJSON(uri, function(result){
				        	layer.close(index); 
							if(result.status == 1){
								$.success(result.msg);
								setTimeout(function(){
									if(callback != undefined){
										eval(callback);
									}else{
										if(result.data && result.data.jumpUrl){
											redirct(result.data.jumpUrl);
										}else{
											window.location.reload();
										}
									}
								},500);
							}else{
								$.error(result.msg);
							}
						});
				        return false;
				    }, function(index){
				    	layer.close(index); 
					});
				});
			},
			initAjaxFormSubmit:function(){
				$('input.J_ajaxSubmit').on('click',function(){
					var $this = $(this),$form = $this.parentsUntil('form'),beforeSend = $this.attr('beforeSend'),
					    callback = $this.attr('onSuccessCallback'),loginCheck = $this.attr('loginCheck');
					if(loginCheck == '1' && !$.weiApp.isLogin())return;
					//parentsUntil 有bug？
					while(!$form.is('form')){
						$form = $form.parent();
					}
					//验证
					if(!$form.doFormValidator()){
						return;
					}
					$this.attr('disabled',true);
					var loading = layer.load();
					$form.ajaxSubmit({
					    dataType:"JSON",
					    type: "post",
				        url: $form.attr('action') ,
				        beforeSend: function(){
				        	if(beforeSend != undefined){
							    eval(beforeSend +'()');
				            }
				        },
				        complete: function(){layer.close(loading);},
				        success: function(data){
				        if(data.status!=0){
				             $.success(data.msg);
				             data = data.data;
				             if(callback != undefined){
							    eval(callback +'($this,data)');
				             }
				             if(data && data.jumpUrl){
				            	 setTimeout(function(){redirct(data.jumpUrl);},500);
							 }
				          }else{
				        	 $this.removeAttr('disabled');
				             $.error(data.msg);
				             if(data.data && 'action:doLogin' == data.data){
				        		 setTimeout(function(){$.weiApp.isLogin();},500);
				        	 }
				          }
				        },
				        error: function(XmlHttpRequest, textStatus, errorThrown){
				        	$this.removeAttr('disabled');
				            $.error('error:'+textStatus);
				        }
				     });
				});
			},
			initToolBtn: function(){
				$(".win-homepage").click(function(){ 
			        if(document.all){
			        document.body.style.behavior = 'url(#default#homepage)'; 
			        document.body.setHomePage(document.URL); 
			        }else{alert("设置首页失败，请手动设置！");} 
				});
				$(".win-favorite").click(function(){
					var sURL=document.URL; 
					var sTitle=document.title; 
					try {window.external.addFavorite(sURL, sTitle);} 
					catch(e){ 
						try{window.sidebar.addPanel(sTitle, sURL, "");} 
						catch(e){$.error("加入收藏失败，请使用Ctrl+D进行添加");} 
					}
				});
			}
	};
})(jQuery);


function jsCopy(e){ 
	try {
		e.select(); //选择对象 
		document.execCommand("Copy"); //执行浏览器复制命令
		$.success("已复制到剪切板"); 
	} catch (e) {
		$.error("您的浏览器不支持，请按ctrl+C手动复制");
	}
} 

function redirct(url){
	url = url || location.href;
	url = url.replace("?__rnd=","__rnd=").replace("&__rnd=","__rnd=").split("__rnd=")[0];
	var rnd = (url.indexOf("?")>0 ? "&" : "?") + "__rnd=" + new Date().getTime();
	window.location.href = url + rnd;
}