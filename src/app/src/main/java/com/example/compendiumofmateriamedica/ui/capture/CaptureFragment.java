package com.example.compendiumofmateriamedica.ui.capture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.compendiumofmateriamedica.MainActivity;
import com.example.compendiumofmateriamedica.PostShareActivity;
import com.example.compendiumofmateriamedica.R;
import com.example.compendiumofmateriamedica.SearchedResults;
import com.example.compendiumofmateriamedica.databinding.FragmentCaptureBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.example.compendiumofmateriamedica.EmptySearchResult;

import model.Plant;
import model.PlantTreeManager;
import model.Post;
import model.PostTreeManager;
import model.RBTreeNode;
import model.SearchGrammarParser;
import model.Token;
import model.Tokenizer;
import model.User;

/**
 * @author: Hongjun Xu
 * @datetime: 2024/4/27
 * @description: Capture Page
 */
public class CaptureFragment extends Fragment {
    // =========== UI变量 ===========
    private FragmentCaptureBinding binding;

    private TextView greeting;
    private EditText searchText;
    private ImageButton captureButton;
    private User currentUser;
    // =========== 当前搜索方法设定 ===========
    private Boolean searchMethod;
    private Spinner spinner;
    private Switch plantPostSwitch;
    private boolean isPost = false;
    private ArrayAdapter<CharSequence> currentArrayAdapter;
    private ArrayList<String> plantAttributes = new ArrayList<>(Arrays.asList(new String[]{"ID", "COMMON_NAME", "SLUG", "SCIENTIFIC_NAME", "GENUS", "FAMILY"}));
    private ArrayList<String> postAttributes = new ArrayList<>(Arrays.asList(new String[]{"POST_ID", "USER_ID", "PLANT_ID", "TIME", "CONTENT"}));
    // ================ Xing Chen 拍照用
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String currentPhotoPath;

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    dispatchTakePictureIntent();
                } else {
                    // 权限被拒绝,你可以在这里处理,例如显示一个 Toast 提示用户
                    Toast.makeText(getContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
                }
            });

    private ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    Intent intent = new Intent(getActivity(), PostShareActivity.class);
                    intent.putExtra("photoPath", currentPhotoPath);
                    intent.putExtra("User", getActivity().getIntent().getSerializableExtra("User"));
                    startActivity(intent);
                }
            });
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CaptureViewModel captureViewModel =
                new ViewModelProvider(this).get(CaptureViewModel.class);

        binding = FragmentCaptureBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        currentUser = (User) getActivity().getIntent().getSerializableExtra("User");
        // ======================== UI ========================
        greeting = binding.textDashboard;
        // 获取当前用户的名称
        captureViewModel.setGreetingText(getResources().getString(R.string.greeting_msg).replace("[]",
                currentUser.getUsername()));
        captureViewModel.getGreetingText().observe(getViewLifecycleOwner(), greeting::setText);

        // ======================== 搜索逻辑 ========================
        // 搜索内容切换
        plantPostSwitch = binding.plantPostSwitch;
        // 设置Switch的Text
        captureViewModel.setSwitchTextText(getResources().getString(R.string.search_switch));
        captureViewModel.getSwitchText().observe(getViewLifecycleOwner(), plantPostSwitch::setText);
        // 搜索栏文字监听
        searchText = binding.searchBarText;
        // 添加一个下拉菜单
        spinner = binding.plantsAttribute;
        // 设置 spinner 列表
        currentArrayAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.plants_attribute,
                android.R.layout.simple_spinner_item
        );
        currentArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // ======================== 拍照 ========================
        // 拍照按钮
        captureButton = binding.captureButton;


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 检测Switch的状态
        plantPostSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                // True: Post
                if (b) {
                    isPost = true;
                    // Create an ArrayAdapter using the string array and a default spinner layout.
                    currentArrayAdapter = ArrayAdapter.createFromResource(
                            getContext(),
                            R.array.posts_attribute,
                            android.R.layout.simple_spinner_item
                    );
                    // Specify the layout to use when the list of choices appears.
                    currentArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    // Apply the adapter to the spinner.
                    spinner.setAdapter(currentArrayAdapter);
                    // False: Plant
                } else {
                    isPost = false;
                    // Create an ArrayAdapter using the string array and a default spinner layout.
                    currentArrayAdapter = ArrayAdapter.createFromResource(
                            getContext(),
                            R.array.plants_attribute,
                            android.R.layout.simple_spinner_item
                    );
                    // Specify the layout to use when the list of choices appears.
                    currentArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    // Apply the adapter to the spinner.
                    spinner.setAdapter(currentArrayAdapter);
                }
            }
        });
        // 初始化spinner
        spinner.setAdapter(currentArrayAdapter);


        /**
         * 添加输入框监听，当输入完成时直接进行搜索， 删除原有的按钮搜索方法
         */
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                // 隐藏键盘
                InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                Log.println(Log.ASSERT, "DEBUG", "[OnClick] Get input: " + textView.getText() + " Select [" + spinner.getSelectedItemId() + "]");
                // 如果是空的不进行处理
                if (textView.getText().toString().trim().equals("")) {
                    textView.setText("");
                    return false;
                }

                // =============================================================================
                // Search with grammar
                // =============================================================================
//                PlantTreeManager plantTreeManager = new PlantTreeManager(((MainActivity) requireActivity()).getPlantTree());
//                PostTreeManager postTreeManager = new PostTreeManager(((MainActivity) requireActivity()).getPostTree());
                // 如果spinner选择的不是第一项【语法搜索】，反之按照当前选择搜索
                if (spinner.getSelectedItemId() == 0) {
                    // 反之进行语法判定逻辑
                    try {
                        // Search with grammar
                        Tokenizer tokenizer = new Tokenizer(textView.getText().toString());
                        SearchGrammarParser searchGrammarParser = new SearchGrammarParser(tokenizer);
                        Map<String, String> searchParam = searchGrammarParser.parseExp();
                        searchMethod = searchGrammarParser.getSearchMethod(); // otherwise AND
                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] Search with grammar");
                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] Found searchParam has "
                                + searchParam.size() + " entities with " + (searchMethod ? "OR" : "AND"));
                        Toast.makeText(requireActivity().getApplicationContext() ,"Search with grammar", Toast.LENGTH_LONG).show();

                        // 遍历搜索attribute
                        Map<RBTreeNode<?>, Integer> searchResult = new HashMap<>();

                        for (Map.Entry<String, String> entry : searchParam.entrySet()) {
                            Log.println(Log.ASSERT, "DEBUG", "[OnClick] search: " + entry.getKey() +
                                    " value: " + entry.getValue());
                            // 生成文件树
                            ArrayList temp;
                            // 如果是搜索植物
                            if (!isPost) {
                                int index = plantAttributes.indexOf(entry.getKey());
                                // 如果没有匹配的则忽略
                                if (index == -1) continue;
                                temp = PlantTreeManager.instance.search(
                                        PlantTreeManager.PlantInfoType.values()[index], entry.getValue());
                                // 添加搜索结果
                                for (Object node : temp) {
                                    if (searchResult.containsKey(node)) {
                                        searchResult.put((RBTreeNode<Plant>) node, searchResult.get(node) + 1);
                                    } else {
                                        searchResult.put((RBTreeNode<Plant>) node, 1);
                                    }
                                }
                            } else {
                                int index = postAttributes.indexOf(entry.getKey());
                                Log.println(Log.ASSERT, "DEBUG", "[OnClick] index: " + index);
                                // 如果没有匹配的则忽略
                                if (index == -1) continue;
                                temp = PostTreeManager.instance.search(
                                        PostTreeManager.PostInfoType.values()[index], entry.getValue());
                                // 添加搜索结果
                                for (Object node : temp) {
                                    if (searchResult.containsKey(node)) {
                                        searchResult.put((RBTreeNode<Post>) node, searchResult.get(node) + 1);
                                    } else {
                                        searchResult.put((RBTreeNode<Post>) node, 1);
                                    }
                                }
                            }
                        }

                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] searchResult: " + searchResult.size());
                        searchMethod = true;
                        // 准备跳转数据
                        // 既然Node无法序列化那就用Id list
                        ArrayList<Integer> plantIDList = new ArrayList<>();
                        for (Map.Entry<RBTreeNode<?>, Integer> entry : searchResult.entrySet()) {
                            // 如果是OR直接添加
                            if (searchMethod) {
                                plantIDList.add((isPost ? (RBTreeNode<Post>) entry.getKey() :
                                        (RBTreeNode<Plant>) entry.getKey()).getKey());
                                // 如果是AND 只添加出现次数与attribute size相同的plant
                            } else if (entry.getValue() == searchResult.size()) {
                                plantIDList.add((isPost ? (RBTreeNode<Post>) entry.getKey() :
                                        (RBTreeNode<Plant>) entry.getKey()).getKey());
                            }

                        }
                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] putExtra: " + plantIDList.size());


                        // 跳转界面
                        textView.setText("");
                        if (plantIDList.size() == 0) {
                            Intent noResult = new Intent(getContext(), EmptySearchResult.class);
                            startActivity(noResult);
                            return false;
                        } else {
                            // 跳转界面
                            Intent postIntent = new Intent(getContext(), SearchedResults.class);
                            postIntent.putExtra("isPost", isPost);
                            postIntent.putExtra("idList", plantIDList);
                            startActivity(postIntent);
                            return true;
                        }
                        // =============================================================================
                    } catch (SearchGrammarParser.IllegalProductionException | Token.IllegalTokenException | IllegalAccessException e) {
                        textView.setText("");
                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] catch error: " + e);
                        Toast.makeText(requireActivity().getApplicationContext() ,"Grammar Error", Toast.LENGTH_LONG).show();
                        return false;
                    }
                } else {

                    // =============================================================================
                    // Search without grammar
                    // =============================================================================

                    Log.println(Log.ASSERT, "DEBUG", "[OnClick] Search without grammar");
                    Toast.makeText(requireActivity().getApplicationContext(), "Search without grammar", Toast.LENGTH_LONG).show();

                    // 搜索节点
                    ArrayList<Integer> plantIDList = new ArrayList<>();
                    if (!isPost) {
                        ArrayList<RBTreeNode<Plant>> searchResult = PlantTreeManager.instance.search(
                                PlantTreeManager.PlantInfoType.values()[(int) spinner.getSelectedItemId() - 1], textView.getText().toString().trim());
                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] Search " + PlantTreeManager.PlantInfoType.values()[(int) spinner.getSelectedItemId() - 1]
                                + " with: " + textView.getText().toString().trim());

                        for (RBTreeNode<Plant> node : searchResult) {
                            plantIDList.add(node.getKey());
                        }
                    } else {
                        ArrayList<RBTreeNode<Post>> searchResult = PostTreeManager.instance.search(
                                PostTreeManager.PostInfoType.values()[(int) spinner.getSelectedItemId() - 1], textView.getText().toString().trim());
                        Log.println(Log.ASSERT, "DEBUG", "[OnClick] Search " + PlantTreeManager.PlantInfoType.values()[(int) spinner.getSelectedItemId() - 1]
                                + " with: " + textView.getText().toString().trim());

                        for (RBTreeNode<Post> node : searchResult) {
                            plantIDList.add(node.getKey());
                        }
                    }


                    // 跳转界面
                    Log.println(Log.ASSERT, "DEBUG", "[OnClick] putExtra: " + plantIDList.size());
                    textView.setText("");
                    if (plantIDList.size() == 0) {
                        Intent noResult = new Intent(getContext(), EmptySearchResult.class);
                        startActivity(noResult);
                        return false;
                    } else {
                        // 跳转界面
                        Intent postIntent = new Intent(getContext(), SearchedResults.class);
                        postIntent.putExtra("isPost", isPost);
                        postIntent.putExtra("idList", plantIDList);
                        startActivity(postIntent);
                        return true;
                    }
                }
            }
        });

        // ===================== Xing Chen: 测试用，如果点击拍照按钮，会直接跳转至post share页面，并传入当前用户
        captureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Log.d("CaptureFragment", "Capture button clicked");
                // 启动相机
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("CaptureFragment", "Request for camera permission");
                    // 请求权限
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                    Log.d("CaptureFragment", "Camera permission grated.");
                } else {
                    Log.d("CaptureFragment", "call dispatchTakePictureIntent()");
                    // 启动相机
                    dispatchTakePictureIntent();
                }
            }
        });

        // ===================== Xing Chen： 到这里结束
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        currentArrayAdapter = null;
    }
    // ===================== Xing Chen: 测试用，点击拍照会进入拍照界面
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            Log.d("CaptureFragment", "Camera app is available");
            // 创建文件来保存图片
            File photoFile = null;
            try {
                Log.d("CaptureFragment", "call createImageFile");
                photoFile = createImageFile();
            } catch (IOException ex) {
                // 错误处理
                Log.d("CaptureFragment", "Error creating file: " + ex.getMessage());
            }
            // 继续只有当文件被成功创建
            Log.d("CaptureFragment", "Camera intent is about to launch");
            if (photoFile != null) {
                Log.d("CaptureFragment", "File created: " + photoFile.getAbsolutePath());
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.compendiumofmateriamedica.fileprovider",
                        photoFile);
                Log.d("CaptureFragment", "URI created");
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                Log.d("CaptureFragment", "Launching camera");
                cameraLauncher.launch(takePictureIntent);
            } else {
                Log.d("CaptureFragment", "Failed to create file");
            }
        } else{
            Log.w("CaptureFragment", "takePictureIntent.resolveActivity(getActivity().getPackageManager()) is null");
        }
    }

    private File createImageFile() throws IOException {
        // 创建一个唯一的文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */
        );

        // 保存文件:路径用于与Intent数据一起传递
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // ===================== Xing Chen： 到这里结束
}